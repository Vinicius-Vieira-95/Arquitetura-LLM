<#
.SYNOPSIS
    Compara os 5 cenarios arquiteturais deste repositorio (Function Calling, ReAct,
    RAG, RAG+Function Calling e RAG+ReAct) rodando os mesmos conjuntos de perguntas
    contra as aplicacoes correspondentes e medindo latencia, numero de idas ao
    modelo (iteracoes/steps) e o comportamento observado (ferramentas/acoes
    escolhidas, chunks recuperados).

.DESCRIPTION
    Pressupoe que as aplicacoes que voce quer comparar ja estao rodando, cada uma
    em uma porta distinta (todas usam 8080 por padrao). Suba cada uma em um
    terminal, definindo SERVER_PORT antes de iniciar. A partir da raiz do
    repositorio:

        cd function-calling-demo
        $env:ANTHROPIC_API_KEY = "sua-chave"; $env:SERVER_PORT = "8081"; mvn spring-boot:run

        cd react-agent
        $env:ANTHROPIC_API_KEY = "sua-chave"; $env:SERVER_PORT = "8082"; ./mvnw spring-boot:run

        cd rag-spring-demo
        $env:ANTHROPIC_API_KEY = "sua-chave"; $env:SERVER_PORT = "8083"; mvn spring-boot:run

        cd rag-function-calling
        $env:ANTHROPIC_API_KEY = "sua-chave"; $env:SERVER_PORT = "8084"; mvn spring-boot:run

        cd rag-react-agent
        $env:ANTHROPIC_API_KEY = "sua-chave"; $env:SERVER_PORT = "8085"; mvn spring-boot:run

    Nenhuma das 5 aplicacoes tem modo mock: todas exigem ANTHROPIC_API_KEY (e
    opcionalmente ANTHROPIC_MODEL) definidos antes de subir, sempre chamando a
    Messages API real da Anthropic. Isso torna a comparacao de latencia
    apples-to-apples entre quaisquer aplicacoes que estiverem no ar.

    Depois, na raiz do repositorio:

        ./benchmark/compare-patterns.ps1

    O script nao exige que as 5 estejam no ar: cada cenario e' checado
    individualmente (via /v3/api-docs) e so entra nas tabelas se responder. Rodar
    so 2 ou 3 de uma vez (ex.: FC vs RAG+FC) tambem funciona.

.PARAMETER FunctionCallingUrl
    Base URL do function-calling-demo (padrao http://localhost:8081)
.PARAMETER ReactUrl
    Base URL do react-agent (padrao http://localhost:8082)
.PARAMETER RagUrl
    Base URL do rag-spring-demo (padrao http://localhost:8083)
.PARAMETER RagFunctionCallingUrl
    Base URL do rag-function-calling (padrao http://localhost:8084)
.PARAMETER RagReActUrl
    Base URL do rag-react-agent (padrao http://localhost:8085)
.PARAMETER ToolQuestionsFile
    Arquivo de perguntas (uma por linha, linhas iniciadas com # sao comentarios)
    usado para comparar os cenarios com ferramentas (FC, ReAct, RAG+FC, RAG+ReAct).
    Padrao: benchmark/questions/function-calling-e-react.txt (50 perguntas).
.PARAMETER RagQuestionsFile
    Arquivo de perguntas (mesmo formato) usado contra os cenarios com RAG (RAG,
    RAG+FC, RAG+ReAct). Padrao: benchmark/questions/rag-edital.txt (50 perguntas
    sobre o edital indexado).
.PARAMETER MaxQuestions
    Limite opcional de perguntas por lista, para um smoke test rapido (0 = sem limite, usa todas).
.PARAMETER OutFile
    Caminho do relatorio Markdown gerado (padrao benchmark/results/comparison.md)
#>
param(
    [string]$FunctionCallingUrl = "http://localhost:8081",
    [string]$ReactUrl = "http://localhost:8082",
    [string]$RagUrl = "http://localhost:8083",
    [string]$RagFunctionCallingUrl = "http://localhost:8084",
    [string]$RagReActUrl = "http://localhost:8085",
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
        Write-Warning "$Name nao respondeu em $Url ($($_.Exception.Message)). Pulando esse cenario."
        return $false
    }
}

# Definicao dos 5 cenarios: cada um sabe montar sua propria requisicao e extrair
# iteracoes/acoes do proprio formato de resposta (que difere entre eles — RAG puro
# nao tem ciclo de iteracoes, FC/RAG+FC usam toolCalls[], ReAct/RAG+ReAct usam
# steps[]). Adicionar um 6o cenario no futuro e' so acrescentar uma entrada aqui;
# nada mais no script precisa mudar.
$scenarioDefs = @(
    @{
        Key = "FC"; Label = "Function Calling"; Url = $FunctionCallingUrl
        HealthPath = "/v3/api-docs"; ChatPath = "/api/chat"; BodyField = "message"
        UsesTools = $true; UsesRag = $false
        GetIterations = { param($r) $r.iterations }
        GetActions    = { param($r) ($r.toolCalls | ForEach-Object { $_.tool }) -join ", " }
    },
    @{
        Key = "ReAct"; Label = "ReAct"; Url = $ReactUrl
        HealthPath = "/v3/api-docs"; ChatPath = "/api/agent"; BodyField = "message"
        UsesTools = $true; UsesRag = $false
        GetIterations = { param($r) $r.iterations }
        GetActions    = { param($r) ($r.steps | ForEach-Object { $_.action }) -join ", " }
    },
    @{
        Key = "RAG"; Label = "RAG"; Url = $RagUrl
        HealthPath = "/v3/api-docs"; ChatPath = "/ask"; BodyField = "question"
        UsesTools = $false; UsesRag = $true
        GetIterations = { param($r) 1 }
        GetActions    = { param($r) "$($r.retrieved.Count) chunks" }
    },
    @{
        Key = "RAG+FC"; Label = "RAG + Function Calling"; Url = $RagFunctionCallingUrl
        HealthPath = "/v3/api-docs"; ChatPath = "/api/rag-fc/chat"; BodyField = "message"
        UsesTools = $true; UsesRag = $true
        GetIterations = { param($r) $r.iterations }
        GetActions    = { param($r) ($r.toolCalls | ForEach-Object { $_.tool }) -join ", " }
    },
    @{
        Key = "RAG+ReAct"; Label = "RAG + ReAct"; Url = $RagReActUrl
        HealthPath = "/v3/api-docs"; ChatPath = "/api/rag-react"; BodyField = "message"
        UsesTools = $true; UsesRag = $true
        GetIterations = { param($r) $r.iterations }
        GetActions    = { param($r) ($r.steps | ForEach-Object { $_.action }) -join ", " }
    }
)

Write-Host "Verificando quais cenarios estao no ar..." -ForegroundColor Cyan
$upScenarios = @()
foreach ($def in $scenarioDefs) {
    if (Test-Endpoint -Name $def.Label -Url "$($def.Url)$($def.HealthPath)") {
        Write-Host "  OK: $($def.Label) ($($def.Url))"
        $upScenarios += $def
    }
}
if ($upScenarios.Count -eq 0) {
    Write-Warning "Nenhum cenario respondeu. Suba pelo menos uma das 5 aplicacoes antes de rodar o script."
    return
}

function Invoke-Scenario {
    param($Def, [string]$Question)
    $body = @{ $Def.BodyField = $Question } | ConvertTo-Json
    $timed = Invoke-Timed {
        Invoke-RestMethod -Uri "$($Def.Url)$($Def.ChatPath)" -Method Post `
            -ContentType "application/json; charset=utf-8" -Body $body
    }
    $r = $timed.Result
    return [pscustomobject]@{
        Iteracoes  = & $Def.GetIterations $r
        LatenciaMs = [math]::Round($timed.ElapsedMs, 1)
        Acoes      = & $Def.GetActions $r
        Resposta   = $r.answer
    }
}

function Build-Section {
    param(
        [string]$Title,
        [array]$Scenarios,
        [string[]]$Questions
    )
    $rows = @()
    if ($Scenarios.Count -eq 0 -or $Questions.Count -eq 0) {
        return $rows
    }
    Write-Host "`n== $Title ==" -ForegroundColor Cyan
    Write-Host ("Cenarios: {0}" -f (($Scenarios | ForEach-Object { $_.Label }) -join ", "))
    foreach ($q in $Questions) {
        $results = [ordered]@{}
        foreach ($def in $Scenarios) {
            try {
                $results[$def.Key] = Invoke-Scenario -Def $def -Question $q
            } catch {
                Write-Warning "Falha em $($def.Label) para '$q': $($_.Exception.Message)"
                $results[$def.Key] = $null
            }
        }
        $rows += [pscustomobject]@{ Pergunta = $q; Results = $results }

        Write-Host "- $q"
        foreach ($def in $Scenarios) {
            $res = $results[$def.Key]
            if ($res) {
                Write-Host ("    {0}: {1} iteracoes, {2} ms, [{3}]" -f $def.Label, $res.Iteracoes, $res.LatenciaMs, $res.Acoes)
            }
        }
    }
    return $rows
}

function Add-SectionMarkdown {
    param(
        [System.Collections.Generic.List[string]]$Md,
        [string]$Title,
        [array]$Scenarios,
        [array]$Rows
    )
    if ($Rows.Count -eq 0) {
        return
    }
    $Md.Add("## $Title")
    $Md.Add("")

    $header = "| Pergunta |"
    $sep = "|---|"
    foreach ($def in $Scenarios) {
        $header += " $($def.Label) iteracoes | $($def.Label) latencia (ms) | $($def.Label) acoes |"
        $sep += "---|---|---|"
    }
    $Md.Add($header)
    $Md.Add($sep)

    foreach ($row in $Rows) {
        $line = "| $($row.Pergunta) |"
        foreach ($def in $Scenarios) {
            $res = $row.Results[$def.Key]
            if ($res) {
                $line += " $($res.Iteracoes) | $($res.LatenciaMs) | $($res.Acoes) |"
            } else {
                $line += " - | - | - |"
            }
        }
        $Md.Add($line)
    }
    $Md.Add("")

    $mediasLine = "Latencia media:"
    foreach ($def in $Scenarios) {
        $vals = $Rows | ForEach-Object { $_.Results[$def.Key] } | Where-Object { $_ } | ForEach-Object { $_.LatenciaMs }
        if ($vals.Count -gt 0) {
            $avg = ($vals | Measure-Object -Average).Average
            $mediasLine += " $($def.Label) = $([math]::Round($avg,1)) ms |"
        }
    }
    $Md.Add($mediasLine)
    $Md.Add("")
}

$toolQuestions = Get-Questions -Path $ToolQuestionsFile -Limit $MaxQuestions
$ragQuestions = Get-Questions -Path $RagQuestionsFile -Limit $MaxQuestions
Write-Host "`nCarregadas $($toolQuestions.Count) perguntas de ferramentas e $($ragQuestions.Count) perguntas de RAG."

# Secao A: cenarios com ferramentas (FC, ReAct, RAG+FC, RAG+ReAct) sobre as
# perguntas de clima/calculadora/cambio/conhecimento-geral. RAG puro fica de fora
# (nao tem ferramentas). Isola: adicionar RAG muda o comportamento/latencia do
# ciclo de ferramentas quando a pergunta nao precisa de RAG?
$toolScenarios = $upScenarios | Where-Object { $_.UsesTools }
$toolRows = Build-Section -Title "Ferramentas (FC vs ReAct vs RAG+FC vs RAG+ReAct)" -Scenarios $toolScenarios -Questions $toolQuestions

# Secao B: cenarios com RAG (RAG, RAG+FC, RAG+ReAct) sobre as perguntas do edital
# indexado. FC/ReAct puros ficam de fora (nao tem RAG). Isola: adicionar
# ferramentas muda o comportamento/latencia do RAG quando a pergunta so precisa
# do contexto recuperado?
$ragScenarios = $upScenarios | Where-Object { $_.UsesRag }
$ragRows = Build-Section -Title "RAG (RAG vs RAG+FC vs RAG+ReAct)" -Scenarios $ragScenarios -Questions $ragQuestions

$outDir = Split-Path -Parent $OutFile
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }

$md = New-Object System.Collections.Generic.List[string]
$md.Add("# Comparacao entre os 5 cenarios arquiteturais")
$md.Add("")
$md.Add("Gerado em $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
$md.Add("")
$md.Add("Cenarios no ar nesta execucao: $(($upScenarios | ForEach-Object { $_.Label }) -join ', ')")
$md.Add("")

Add-SectionMarkdown -Md $md -Title "Ferramentas (FC vs ReAct vs RAG+FC vs RAG+ReAct)" -Scenarios $toolScenarios -Rows $toolRows
Add-SectionMarkdown -Md $md -Title "RAG (RAG vs RAG+FC vs RAG+ReAct)" -Scenarios $ragScenarios -Rows $ragRows

if ($toolRows.Count -eq 0 -and $ragRows.Count -eq 0) {
    $md.Add("Nenhum cenario respondeu. Verifique se pelo menos uma das 5 aplicacoes esta rodando na porta configurada.")
}

$md -join "`n" | Out-File -FilePath $OutFile -Encoding utf8

Write-Host "`nRelatorio salvo em $OutFile" -ForegroundColor Green
