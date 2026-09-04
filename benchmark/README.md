# Harness de comparacao entre os 5 cenarios

Script (`compare-patterns.ps1`) que roda os mesmos conjuntos de perguntas contra
as 5 demos do repositorio — `function-calling-demo`, `react-agent`,
`rag-spring-demo`, `rag-function-calling` e `rag-react-agent` — e gera um
relatorio comparando latencia e o "formato" de cada ciclo (numero de
iteracoes/steps, ferramentas ou acoes escolhidas, chunks recuperados no caso
do RAG).

O script nao exige que as 5 estejam no ar ao mesmo tempo: cada cenario e'
checado individualmente (via `/v3/api-docs`) e so entra nas tabelas se
responder. Rodar so 2 ou 3 de uma vez (ex.: `FC` vs `RAG+FC`) tambem funciona.

## Questionarios (50 perguntas cada)

As perguntas ficam em `questions/`, uma por linha (linhas com `#` sao
comentarios/cabecalhos de secao, ignoradas pelo script):

- **`questions/function-calling-e-react.txt`** (50 perguntas) — usado contra os
  4 cenarios com ferramentas: `function-calling-demo`, `react-agent`,
  `rag-function-calling` e `rag-react-agent`. Cobre as 4 categorias que as
  ferramentas deles suportam: clima (13), calculadora (13), cambio (12) e
  conhecimento geral sem ferramenta (12). Versao anotada com o que observar
  em cada categoria: `questions/function-calling-e-react.md`.
- **`questions/rag-edital.txt`** (50 perguntas) — usado contra os 3 cenarios
  com RAG: `rag-spring-demo`, `rag-function-calling` e `rag-react-agent`,
  todas com resposta dentro do corpus indexado (`Edital 2027.1.pdf`, o Edital
  Nº 04/2026-CEV/UECE de isencao da taxa do Vestibular). Versao anotada com o
  item do edital que cada pergunta cobre: `questions/rag-edital.md`.

Para editar/estender: basta adicionar linhas nesses `.txt` (uma pergunta por
linha); o script re-le o arquivo a cada execucao.

## Por que comparar assim (2 secoes, nao 1 tabela de 5 colunas)

O relatorio tem duas secoes independentes, cada uma isolando uma pergunta
diferente:

- **Ferramentas** (`FC`, `ReAct`, `RAG+FC`, `RAG+ReAct` — RAG puro fica de
  fora, nao tem ferramentas) sobre `function-calling-e-react.txt`. Como os 4
  cenarios compartilham exatamente as mesmas ferramentas (clima, calculadora,
  cambio), isola duas variaveis ao mesmo tempo:
  - JSON estruturado (`tool_use`) vs. texto livre parseado
    (Thought/Action/Observation) — `FC` vs `ReAct`, `RAG+FC` vs `RAG+ReAct`;
  - o custo de acrescentar RAG a um ciclo de ferramentas quando a pergunta nao
    precisa de RAG — `FC` vs `RAG+FC`, `ReAct` vs `RAG+ReAct`.
- **RAG** (`RAG`, `RAG+FC`, `RAG+ReAct` — FC/ReAct puros ficam de fora, nao tem
  RAG) sobre `rag-edital.txt`. Isola o custo/beneficio de acrescentar
  ferramentas (em particular `search_documents`) a um ciclo de RAG quando a
  pergunta so precisa do contexto recuperado inicialmente — RAG puro tem
  sempre 1 "iteracao" (chamada unica); RAG+FC/RAG+ReAct podem gastar uma
  iteracao extra se o modelo decidir refinar a busca.

Rodar as mesmas 50+50 perguntas nos 5 cenarios permite comparar qualquer par
ou subconjunto a partir do mesmo relatorio, sem re-rodar nada.

## Como rodar

As 5 aplicacoes usam a porta 8080 por padrao. Para rodar todas ao mesmo tempo,
suba cada uma em um terminal com `SERVER_PORT` diferente, a partir da raiz do
repositorio (nenhuma tem modo mock — todas exigem `ANTHROPIC_API_KEY`):

```bash
cd function-calling-demo
export ANTHROPIC_API_KEY="sua-chave"
SERVER_PORT=8081 mvn spring-boot:run
```

```bash
cd react-agent
export ANTHROPIC_API_KEY="sua-chave"
SERVER_PORT=8082 ./mvnw spring-boot:run
```

```bash
cd rag-spring-demo
export ANTHROPIC_API_KEY="sua-chave"
SERVER_PORT=8083 mvn spring-boot:run
```

```bash
cd rag-function-calling
export ANTHROPIC_API_KEY="sua-chave"
SERVER_PORT=8084 mvn spring-boot:run
```

```bash
cd rag-react-agent
export ANTHROPIC_API_KEY="sua-chave"
SERVER_PORT=8085 mvn spring-boot:run
```

`rag-function-calling` e `rag-react-agent` dependem de `common/` e/ou
`rag-spring-demo` como bibliotecas Maven — instale as duas primeiro:

```bash
cd common && mvn install && cd ..
cd rag-spring-demo && mvn install -Dtest='!RetrievalEvaluationTest' && cd ..
```

(No PowerShell: `$env:ANTHROPIC_API_KEY = "sua-chave"; $env:SERVER_PORT = "8081"; mvn spring-boot:run`.)

Depois, na raiz do repositorio:

```powershell
./benchmark/compare-patterns.ps1
```

O relatorio Markdown e' salvo em `benchmark/results/comparison.md` (a pasta
`results/` e' criada automaticamente e ignorada pelo git).

## Parametros do script

| Parametro | Padrao | Descricao |
|---|---|---|
| `-FunctionCallingUrl` | `http://localhost:8081` | Base URL do function-calling-demo |
| `-ReactUrl` | `http://localhost:8082` | Base URL do react-agent |
| `-RagUrl` | `http://localhost:8083` | Base URL do rag-spring-demo |
| `-RagFunctionCallingUrl` | `http://localhost:8084` | Base URL do rag-function-calling |
| `-RagReActUrl` | `http://localhost:8085` | Base URL do rag-react-agent |
| `-ToolQuestionsFile` | `questions/function-calling-e-react.txt` | Perguntas para os cenarios com ferramentas |
| `-RagQuestionsFile` | `questions/rag-edital.txt` | Perguntas para os cenarios com RAG |
| `-MaxQuestions` | `0` (sem limite, usa as 50) | Limita quantas perguntas de cada lista rodar — util para um smoke test rapido, ex.: `-MaxQuestions 5` |
| `-OutFile` | `benchmark/results/comparison.md` | Caminho do relatorio gerado |

Se algum cenario nao estiver no ar, o script pula ele (avisando no console) em
vez de falhar — as tabelas so incluem colunas dos cenarios que responderam.
