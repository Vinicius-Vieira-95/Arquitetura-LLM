<#
.SYNOPSIS
    Compara os tres padroes arquiteturais deste repositorio (Function Calling,
    ReAct e RAG) rodando um conjunto fixo de perguntas contra as 3 aplicacoes e
    medindo latencia, numero de idas ao modelo (iteracoes/steps) e o
    comportamento observado (ferramentas/acoes escolhidas, chunks recuperados).

.DESCRIPTION
    Pressupoe que as 3 aplicacoes ja estao rodando, cada uma em uma porta
    distinta (todas usam 8080 por padrao). Suba cada uma em um terminal,
    definindo SERVER_PORT antes de iniciar. A partir da raiz do repositorio:

        cd function-calling-demo
        $env:ANTHROPIC_API_KEY = "sua-chave"; $env:SERVER_PORT = "8081"; mvn spring-boot:run

        cd react-agent
        $env:SERVER_PORT = "8082"; ./mvnw spring-boot:run

        cd rag-spring-demo
        $env:SERVER_PORT = "8083"; mvn spring-boot:run

    O function-calling-demo nao tem mais modo mock: ele sempre chama a
    Messages API real da Anthropic, entao exige ANTHROPIC_API_KEY (e
    opcionalmente ANTHROPIC_MODEL) definidos antes de subir. O react-agent
    ainda tem llm.provider=mock (padrao, sem chave) para uma comparacao
    ESTRUTURAL rapida (numero de iteracoes, ferramentas/acoes escolhidas,
    determinismo) — mas nesse caso a comparacao de latencia entre os dois
    deixa de ser apples-to-apples, pois so o function-calling-demo estaria
    pagando latencia de rede real. Para uma comparacao de LATENCIA REAL,
    suba as 2 com ANTHROPIC_API_KEY definido e, no react-agent, tambem
    LLM_PROVIDER=anthropic.

    Depois, na raiz do repositorio:

        ./benchmark/compare-patterns.ps1

.PARAMETER FunctionCallingUrl
    Base URL do function-calling-demo (padrao http://localhost:8081)
.PARAMETER ReactUrl
    Base URL do react-agent (padrao http://localhost:8082)
.PARAMETER RagUrl
    Base URL do rag-spring-demo (padrao http://localhost:8083)
.PARAMETER ToolQuestionsFile
    Arquivo de perguntas (uma por linha, linhas iniciadas com # sao comentarios)
    usado para comparar Function Calling vs ReAct. Padrao:
    benchmark/questions/function-calling-e-react.txt (50 perguntas).
.PARAMETER RagQuestionsFile
    Arquivo de perguntas (mesmo formato) usado contra o RAG. Padrao:
    benchmark/questions/rag-edital.txt (50 perguntas sobre o edital indexado).
.PARAMETER MaxQuestions
    Limite opcional de perguntas por lista, para um smoke test rapido (0 = sem limite, usa todas).
.PARAMETER OutFile
    Caminho do relatorio Markdown gerado (padrao benchmark/results/comparison.md)
#>
param(
    [string]$FunctionCallingUrl = "http://localhost:8081",
    [string]$ReactUrl = "http://localhost:8082",
    [string]$RagUrl = "http://localhost:8083",
    [string]$ToolQuestionsFile = "$PSScriptRoot/questions/function-calling-e-react.txt",
    [string]$RagQuestionsFile = "$PSScriptRoot/questions/rag-edital.txt",
    [int]$MaxQuestions = 0,
    [string]$OutFile = "$PSScriptRoot/results/comparison.md"
)

$ErrorActionPreference = "Stop"

function Get-Questions {
    param([string]$Path, [int]$Limit)
    if (-not (Test-Path $Path)) {
        throw "Arquivo de perguntas nao encontrado: $Path"
    }
    $lines = Get-Content -Path $Path -Encoding UTF8 |
        ForEach-Object { $_.Trim() } |
        Where-Object { $_ -ne "" -and -not $_.StartsWith("#") }
    if ($Limit -gt 0) {
        $lines = $lines | Select-Object -First $Limit
    }
    return $lines
}

# As 50 perguntas de function-calling-e-react.txt sao identicas para os dois
# projetos: eles compartilham exatamente as mesmas ferramentas (WeatherTool,
# CalculatorTool, CurrencyTool), entao a comparacao e' apples-to-apples.
$toolQuestions = Get-Questions -Path $ToolQuestionsFile -Limit $MaxQuestions

# As 50 perguntas de rag-edital.txt sao sobre o corpus real indexado pelo RAG
# (rag-spring-demo/src/main/resources/docs/Edital 2027.1.pdf). Veja
# benchmark/questions/rag-edital.md para a referencia de qual item do edital
# cada pergunta cobre.
$ragQuestions = Get-Questions -Path $RagQuestionsFile -Limit $MaxQuestions

Write-Host "Carregadas $($toolQuestions.Count) perguntas de Function Calling/ReAct e $($ragQuestions.Count) perguntas de RAG."

function Invoke-Timed {
    param([scriptblock]$Action)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $result = & $Action
    $sw.Stop()
    return [pscustomobject]@{ Result = $result; ElapsedMs = $sw.Elapsed.TotalMilliseconds }
}

function Test-Endpoint {
    param([string]$Name, [string]$Url)
    try {
        Invoke-RestMethod -Uri $Url -Method Get -TimeoutSec 3 | Out-Null
        return $true
    } catch {
        Write-Warning "$Name nao respondeu em $Url ($($_.Exception.Message)). Pulando essa aplicacao."
        return $false
    }
}

$fcUp = Test-Endpoint -Name "function-calling-demo" -Url "$FunctionCallingUrl/v3/api-docs"
$reactUp = Test-Endpoint -Name "react-agent" -Url "$ReactUrl/v3/api-docs"
$ragUp = Test-Endpoint -Name "rag-spring-demo" -Url "$RagUrl/health"

$rows = @()

if ($fcUp -and $reactUp) {
    Write-Host "`n== Function Calling vs ReAct ==" -ForegroundColor Cyan
    foreach ($q in $toolQuestions) {
        try {
            $fc = Invoke-Timed { Invoke-RestMethod -Uri "$FunctionCallingUrl/api/chat" -Method Post `
                -ContentType "application/json; charset=utf-8" -Body (@{ message = $q } | ConvertTo-Json) }
            $re = Invoke-Timed { Invoke-RestMethod -Uri "$ReactUrl/api/agent" -Method Post `
                -ContentType "application/json; charset=utf-8" -Body (@{ message = $q } | ConvertTo-Json) }

            $fcTools = ($fc.Result.toolCalls | ForEach-Object { $_.tool }) -join ", "
            $reActions = ($re.Result.steps | ForEach-Object { $_.action }) -join ", "

            $row = [pscustomobject]@{
                Pergunta         = $q
                FC_Iteracoes     = $fc.Result.iterations
                FC_Ferramentas   = $fcTools
                FC_LatenciaMs    = [math]::Round($fc.ElapsedMs, 1)
                FC_Resposta      = $fc.Result.answer
                React_Iteracoes  = $re.Result.iterations
                React_Acoes      = $reActions
                React_LatenciaMs = [math]::Round($re.ElapsedMs, 1)
                React_Resposta   = $re.Result.answer
            }
            $rows += $row
            Write-Host "- $q"
            Write-Host ("    Function Calling: {0} iteracoes, {1} ms, ferramentas=[{2}]" -f $row.FC_Iteracoes, $row.FC_LatenciaMs, $fcTools)
            Write-Host ("    ReAct:            {0} iteracoes, {1} ms, acoes=[{2}]" -f $row.React_Iteracoes, $row.React_LatenciaMs, $reActions)
        } catch {
            Write-Warning "Falha ao comparar a pergunta '$q': $($_.Exception.Message)"
        }
    }
} else {
    Write-Warning "Pulando comparacao Function Calling vs ReAct (uma das duas aplicacoes nao esta no ar)."
}

$ragRows = @()
if ($ragUp) {
    Write-Host "`n== RAG ==" -ForegroundColor Cyan
    foreach ($q in $ragQuestions) {
        try {
            $rag = Invoke-Timed { Invoke-RestMethod -Uri "$RagUrl/ask" -Method Post `
                -ContentType "application/json; charset=utf-8" -Body (@{ question = $q } | ConvertTo-Json) }
            $ragRow = [pscustomobject]@{
                Pergunta     = $q
                ChunksUsados = $rag.Result.retrieved.Count
                LatenciaMs   = [math]::Round($rag.ElapsedMs, 1)
                Resposta     = $rag.Result.answer
            }
            $ragRows += $ragRow
            Write-Host ("- {0} -> {1} chunks, {2} ms" -f $q, $ragRow.ChunksUsados, $ragRow.LatenciaMs)
        } catch {
            Write-Warning "Falha ao consultar o RAG para '$q': $($_.Exception.Message)"
        }
    }
} else {
    Write-Warning "Pulando RAG (aplicacao nao esta no ar)."
}

# Monta o relatorio Markdown.
$outDir = Split-Path -Parent $OutFile
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }

$md = New-Object System.Collections.Generic.List[string]
$md.Add("# Comparacao entre padroes arquiteturais para LLM")
$md.Add("")
$md.Add("Gerado em $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$md.Add("")

if ($rows.Count -gt 0) {
    $md.Add("## Function Calling vs ReAct")
    $md.Add("")
    $md.Add("| Pergunta | FC iteracoes | FC latencia (ms) | FC ferramentas | ReAct iteracoes | ReAct latencia (ms) | ReAct acoes |")
    $md.Add("|---|---|---|---|---|---|---|")
    foreach ($r in $rows) {
        $md.Add("| $($r.Pergunta) | $($r.FC_Iteracoes) | $($r.FC_LatenciaMs) | $($r.FC_Ferramentas) | $($r.React_Iteracoes) | $($r.React_LatenciaMs) | $($r.React_Acoes) |")
    }
    $md.Add("")
    $avgFc = ($rows | Measure-Object -Property FC_LatenciaMs -Average).Average
    $avgRe = ($rows | Measure-Object -Property React_LatenciaMs -Average).Average
    $md.Add("Latencia media: Function Calling = $([math]::Round($avgFc,1)) ms | ReAct = $([math]::Round($avgRe,1)) ms")
    $md.Add("")
}

if ($ragRows.Count -gt 0) {
    $md.Add("## RAG")
    $md.Add("")
    $md.Add("| Pergunta | Chunks usados | Latencia (ms) |")
    $md.Add("|---|---|---|")
    foreach ($r in $ragRows) {
        $md.Add("| $($r.Pergunta) | $($r.ChunksUsados) | $($r.LatenciaMs) |")
    }
    $md.Add("")
}

if ($rows.Count -eq 0 -and $ragRows.Count -eq 0) {
    $md.Add("Nenhuma aplicacao respondeu. Verifique se as 3 demos estao rodando nas portas configuradas.")
}

$md -join "`n" | Out-File -FilePath $OutFile -Encoding utf8

Write-Host "`nRelatorio salvo em $OutFile" -ForegroundColor Green