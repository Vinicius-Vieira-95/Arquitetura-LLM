# Harness de comparacao entre os 3 padroes

Script (`compare-patterns.ps1`) que roda o mesmo conjunto de perguntas contra as
3 demos do repositorio — `function-calling-demo`, `react-agent` e
`rag-spring-demo` — e gera um relatorio comparando latencia e o "formato" de
cada ciclo (numero de iteracoes/steps, ferramentas ou acoes escolhidas, chunks
recuperados no caso do RAG).

## Questionarios (50 perguntas cada)

As perguntas ficam em `questions/`, uma por linha (linhas com `#` sao
comentarios/cabecalhos de secao, ignoradas pelo script):

- **`questions/function-calling-e-react.txt`** (50 perguntas) — usado contra
  `function-calling-demo` e `react-agent`. Cobre as 4 categorias que as
  ferramentas deles suportam: clima (13), calculadora (13), cambio (12) e
  conhecimento geral sem ferramenta (12). Versao anotada com o que observar
  em cada categoria: `questions/function-calling-e-react.md`.
- **`questions/rag-edital.txt`** (50 perguntas) — usado contra `rag-spring-demo`,
  todas com resposta dentro do corpus indexado (`Edital 2027.1.pdf`, o Edital
  Nº 04/2026-CEV/UECE de isencao da taxa do Vestibular). Versao anotada com o
  item do edital que cada pergunta cobre: `questions/rag-edital.md`.

Para editar/estender: basta adicionar linhas nesses `.txt` (uma pergunta por
linha); o script re-le o arquivo a cada execucao.

## Por que comparar assim

- `function-calling-demo` e `react-agent` compartilham exatamente as mesmas
  ferramentas (clima, calculadora, cambio), entao rodar as mesmas perguntas
  contra os dois isola a variavel arquitetural: JSON estruturado (`tool_use`)
  vs. texto livre parseado (Thought/Action/Observation).
- `rag-spring-demo` nao usa ferramentas, entao e' comparado separadamente com
  perguntas sobre o corpus indexado. Se voce trocar o PDF/markdown em
  `rag-spring-demo/src/main/resources/docs`, atualize `questions/rag-edital.txt`
  para refletir o novo conteudo.

## Como rodar

As 3 aplicacoes usam a porta 8080 por padrao. Para rodar as 3 ao mesmo tempo,
suba cada uma em um terminal com `SERVER_PORT` diferente, a partir da raiz do
repositorio:

```bash
cd function-calling-demo
export ANTHROPIC_API_KEY="sua-chave"   # obrigatorio: nao tem mais modo mock
SERVER_PORT=8081 mvn spring-boot:run
```

```bash
cd react-agent
SERVER_PORT=8082 ./mvnw spring-boot:run
```

```bash
cd rag-spring-demo
SERVER_PORT=8083 mvn spring-boot:run
```

(No PowerShell: `$env:SERVER_PORT = "8081"; mvn spring-boot:run`.)

Depois, na raiz do repositorio:

```powershell
./benchmark/compare-patterns.ps1
```

O relatorio Markdown e' salvo em `benchmark/results/comparison.md` (a pasta
`results/` e' criada automaticamente e ignorada pelo git).

## Dois modos de comparacao

O `function-calling-demo` e o `rag-spring-demo` nao tem modo mock: as duas
sempre chamam a Messages API real da Anthropic, entao exigem
`ANTHROPIC_API_KEY` definido no processo (senao os endpoints respondem 500 e o
script pula essas perguntas, avisando no console). Ja o `react-agent` ainda
tem `llm.provider=mock` (padrao, sem chave).

- **Latencia real (recomendado)**: suba as 3 com `ANTHROPIC_API_KEY` definido
  e, no react-agent, tambem `LLM_PROVIDER=anthropic`. Mede o custo real de
  rede/tokens de cada padrao, incluindo o overhead de parsing por regex do
  ReAct vs. o `tool_use` nativo do Function Calling — e' o modo apples-to-apples
  para comparar latencia entre os 3.
- **Estrutural, so no react-agent (`llm.provider=mock`, sem chave)**: util para
  inspecionar rapido e de graca o numero de acoes/steps do ReAct de forma
  deterministica. Como o function-calling-demo continua pagando latencia de
  rede real, a coluna de latencia da comparacao deixa de ser apples-to-apples
  nesse modo — use so para olhar iteracoes/acoes escolhidas, nao latencia.

## Parametros do script

| Parametro | Padrao | Descricao |
|---|---|---|
| `-FunctionCallingUrl` | `http://localhost:8081` | Base URL do function-calling-demo |
| `-ReactUrl` | `http://localhost:8082` | Base URL do react-agent |
| `-RagUrl` | `http://localhost:8083` | Base URL do rag-spring-demo |
| `-ToolQuestionsFile` | `questions/function-calling-e-react.txt` | Perguntas para FC vs ReAct |
| `-RagQuestionsFile` | `questions/rag-edital.txt` | Perguntas para o RAG |
| `-MaxQuestions` | `0` (sem limite, usa as 50) | Limita quantas perguntas de cada lista rodar — util para um smoke test rapido, ex.: `-MaxQuestions 5` |
| `-OutFile` | `benchmark/results/comparison.md` | Caminho do relatorio gerado |

Se alguma aplicacao nao estiver no ar, o script pula essa comparacao (avisando
no console) em vez de falhar.