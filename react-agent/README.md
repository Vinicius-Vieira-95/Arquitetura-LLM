# React Agent Demo (Spring Boot)

Projeto didático que implementa os **princípios do padrão ReAct** (Reasoning + Acting, Yao et al. 2022) de ponta a ponta em Spring Boot, com dois modos de execução:

- **`mock`** — modelo simulado e determinístico, roda sem chave de API;
- **`anthropic`** — chamadas reais à Messages API da Anthropic.

O orquestrador (`ReActAgentService`) é idêntico nos dois modos: ele não sabe qual implementação de modelo está por trás — só sabe pedir a continuação de um texto e parsear o resultado.

---

## O princípio central

ReAct é diferente de Function Calling (veja o projeto irmão `function-calling-demo`): ali o modelo devolve um bloco **estruturado** (`tool_use`, com argumentos já em JSON). Em ReAct, o modelo produz apenas **texto livre** seguindo um formato fixo, e é a aplicação quem interpreta esse texto:

```
Thought: <raciocínio do modelo sobre o que fazer>
Action: <nome de uma ferramenta>
Action Input: <texto livre para a ferramenta>
Observation: <preenchido pela aplicação, após executar a ferramenta de verdade>
... (o padrão Thought/Action/Action Input/Observation se repete quantas vezes for preciso)
Thought: agora eu sei a resposta final
Final Answer: <resposta ao usuário>
```

O laço:

1. a aplicação monta o prompt: instruções do formato + lista de ferramentas + a pergunta + o "scratchpad" (tudo que já foi gerado até agora);
2. pede ao modelo para **continuar** esse texto, com uma *stop sequence* em `"\nObservation:"` — assim o modelo nunca inventa o resultado da ferramenta;
3. se a continuação contiver `Final Answer:`, o ciclo termina;
4. caso contrário, a aplicação extrai `Action` e `Action Input` da continuação, **executa a ferramenta de verdade**, anexa `Observation: <resultado>\nThought:` ao scratchpad e repete o laço.

```
Usuário → [ReActAgentService] → Modelo (continua o texto até "Observation:")
                 ↑                        │  (Action + Action Input em texto livre)
      (Observation: <resultado>)          ▼
                 [ToolRegistry executa a Tool]
```

### Uma nuance da API atual

A Messages API é orientada a chat e não aceita mais *prefill* no último turno do assistente nos modelos atuais (retorna 400). Por isso o prompt inteiro — instruções + scratchpad acumulado — é enviado como uma única mensagem `user`, com instruções explícitas pedindo ao modelo que responda **apenas com a continuação do texto**, como se estivesse preenchendo o próximo trecho da transcrição. Veja `AnthropicLlmClient`.

---

## Arquitetura

| Camada | Classes | Papel |
|--------|---------|-------|
| Ferramentas | `Tool`, `ToolRegistry`, `WeatherTool`, `CalculatorTool`, `CurrencyTool` | Funções executáveis; cada uma interpreta seu próprio Action Input em texto livre |
| Modelo (LLM) | `LlmClient`, `MockLlmClient`, `AnthropicLlmClient` | Abstração de "continuar um texto", com duas implementações |
| Orquestração | `ReActAgentService`, `AgentStep`, `AgentResult` | O laço Thought → Action → Observation → ... → Final Answer |
| Web | `AgentController`, DTOs | Endpoint REST |

Adicionar uma ferramenta nova é só criar uma classe `@Component` que implemente `Tool`. O `ToolRegistry` a injeta automaticamente e ela passa a ser descrita no prompt — sem tocar no orquestrador.

---

## Como rodar

Requisitos: **JDK 25** e Maven (ou o `mvnw` incluso).

### Modo mock (sem chave)

```bash
./mvnw spring-boot:run
```

O provedor padrão é `mock`. Para ver o ciclo pelo console durante o startup:

```bash
DEMO_RUN=true ./mvnw spring-boot:run
```

### Modo Anthropic (API real)

```bash
export ANTHROPIC_API_KEY="sua-chave"
export LLM_PROVIDER=anthropic
export ANTHROPIC_MODEL=claude-sonnet-5   # ou outro modelo a que você tenha acesso
./mvnw spring-boot:run
```

### Testes

```bash
./mvnw test
```

---

## Documentação Swagger/OpenAPI

Com a aplicação rodando, a documentação interativa fica em:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

---

## Usando o endpoint

```bash
curl -s http://localhost:8080/api/agent \
  -H "Content-Type: application/json" \
  -d '{"message": "Quanto é (12 + 7) * 3?"}'
```

Resposta (repare no rastro Thought/Action/Observation, que evidencia o ciclo ReAct):

```json
{
  "answer": "(12 + 7) * 3 = 57",
  "steps": [
    {
      "thought": "Preciso calcular essa expressão.",
      "action": "calculate",
      "actionInput": "(12 + 7) * 3",
      "observation": "(12 + 7) * 3 = 57",
      "isError": false
    }
  ],
  "iterations": 2
}
```

Outras perguntas de exemplo:

- `"Qual o clima em Fortaleza?"` → aciona `get_weather`
- `"Converta 100 dólares em reais"` → aciona `convert_currency`
- `"Quem descobriu o Brasil?"` → o modelo responde direto, sem ferramenta

---

## Configuração (`application.yaml`)

| Propriedade | Env | Padrão | Descrição |
|-------------|-----|--------|-----------|
| `llm.provider` | `LLM_PROVIDER` | `mock` | `mock` ou `anthropic` |
| `react.max-iterations` | — | `5` | Limite de idas ao modelo por pergunta |
| `anthropic.api-key` | `ANTHROPIC_API_KEY` | — | Chave da API (não versione!) |
| `anthropic.model` | `ANTHROPIC_MODEL` | `claude-sonnet-5` | Modelo usado |
| `demo.run-on-startup` | `DEMO_RUN` | `false` | Roda exemplos no console |
| `server.port` | `SERVER_PORT` | `8080` | Porta HTTP (util para rodar as 3 demos do repo ao mesmo tempo) |

---

## Observabilidade

Com o Actuator, a aplicacao expoe:

- `GET /actuator/health` — saude da aplicacao;
- `GET /actuator/metrics` — lista de metricas disponiveis;
- `GET /actuator/metrics/{nome}` — detalhe de uma metrica, ex.: `react.run`.

Metricas publicadas pelo `ReActAgentService` (via Micrometer):

| Metrica | Tipo | Tags | O que mede |
|---------|------|------|------------|
| `react.run` | timer | `outcome` (`success`/`iteration_limit`/`error`) | Duracao do ciclo completo de `run()` |
| `react.tool` | timer | `tool`, `outcome` | Duracao de cada execucao de ferramenta |
| `react.iterations` | summary | — | Distribuicao do numero de idas ao modelo por pergunta |
| `react.errors` | counter | — | Excecoes nao tratadas durante o ciclo |

---

## Comparando com o projeto irmao (function-calling-demo)

Ha um harness em `../benchmark/compare-patterns.ps1` que roda o mesmo conjunto
de perguntas contra este projeto e o `function-calling-demo`, comparando
latencia e numero de iteracoes lado a lado — util para o capitulo de
resultados do TCC. Veja `../benchmark/README.md`.

---

## Mapa: conceito ReAct → código

| Conceito | Onde está no código |
|----------|----------------------|
| Formato Thought/Action/Action Input/Observation | Prompt montado em `ReActAgentService.buildPrompt` |
| "Scratchpad" (transcrição acumulada) | `ReActAgentService.run` — `StringBuilder scratchpad` |
| Parada antes de alucinar a Observation | `stop_sequences: ["\nObservation:"]` em `AnthropicLlmClient` |
| Parsing da Action/Action Input | Regex em `ReActAgentService` |
| Execução real da ferramenta | `ToolRegistry.execute(...)` |
| Detecção da resposta final | Busca por `"Final Answer:"` no texto gerado |

> Comparado ao `function-calling-demo`: lá o modelo devolve JSON estruturado (`tool_use`); aqui o modelo devolve texto livre que a aplicação precisa parsear — essa é a diferença fundamental entre os dois padrões.
