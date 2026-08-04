# Function Calling Demo (Spring Boot)

Projeto didático que implementa os **princípios de Function Calling** (tool use) de ponta a ponta em Spring Boot, com dois modos de execução:

- **`mock`** — modelo simulado e determinístico, roda sem chave de API;
- **`anthropic`** — chamadas reais à Messages API da Anthropic.

O orquestrador (`ChatService`) é idêntico nos dois modos: ele não sabe qual implementação de modelo está por trás. Isso é justamente o que se quer demonstrar — o padrão independe do provedor.

---

## O princípio central

Em Function Calling, **o modelo nunca executa a função**. O ciclo é:

1. A aplicação envia ao modelo a pergunta do usuário **e** a lista de ferramentas disponíveis (nome, descrição e um *JSON Schema* dos parâmetros).
2. O modelo decide se precisa de uma ferramenta. Se sim, responde com um bloco `tool_use` contendo o nome da ferramenta e os argumentos já estruturados.
3. A **aplicação** executa a função de verdade e devolve o resultado ao modelo num bloco `tool_result`.
4. O modelo usa o resultado para produzir a resposta final em linguagem natural — ou pede outra ferramenta, repetindo o ciclo.

O laço se repete até o modelo responder só com texto (`stop_reason != "tool_use"`) ou até um limite de iterações.

```
Usuário → [ChatService] → Modelo
                 ↑            │  (tool_use: name + input)
     (tool_result)           ▼
             [ToolRegistry executa a Tool]
```

---

## Arquitetura

| Camada | Classes | Papel |
|--------|---------|-------|
| Modelo de domínio | `Message`, `ContentBlock` (`Text` / `ToolUse` / `ToolResult`), `Role`, `ToolSpec` | Representação neutra da conversa, independente do provedor |
| Ferramentas | `Tool`, `ToolRegistry`, `WeatherTool`, `CalculatorTool`, `CurrencyTool` | Funções executáveis + suas especificações publicadas ao modelo |
| Modelo (LLM) | `LlmClient`, `MockLlmClient`, `AnthropicLlmClient` | Abstração do modelo com duas implementações |
| Orquestração | `ChatService` | O *agentic loop* de Function Calling |
| Web | `ChatController`, DTOs | Endpoint REST |

Adicionar uma ferramenta nova é só criar uma classe `@Component` que implemente `Tool`. O `ToolRegistry` a injeta automaticamente e ela passa a ser oferecida ao modelo — sem tocar no orquestrador.

---

## Como rodar

Requisitos: **JDK 21** e **Maven**.

### Modo mock (sem chave)

```bash
mvn spring-boot:run
```

O provedor padrão é `mock`. Para ver o ciclo pelo console durante o startup:

```bash
DEMO_RUN=true mvn spring-boot:run
```

### Modo Anthropic (API real)

```bash
export ANTHROPIC_API_KEY="sua-chave"
export LLM_PROVIDER=anthropic
export ANTHROPIC_MODEL=claude-sonnet-5   # ou outro modelo a que você tenha acesso
mvn spring-boot:run
```

### Testes

```bash
mvn test
```

---

## Usando o endpoint

```bash
curl -s http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Quanto é (12 + 7) * 3?"}'
```

Resposta (repare no rastro de ferramentas, que evidencia o ciclo):

```json
{
  "answer": "Pronto! Resultado das ferramentas:\n- (12 + 7) * 3 = 57",
  "toolCalls": [
    {
      "tool": "calculate",
      "input": { "expression": "(12 + 7) * 3" },
      "output": "(12 + 7) * 3 = 57",
      "isError": false
    }
  ],
  "iterations": 2
}
```

Outras perguntas de exemplo:

- `"Qual o clima em Fortaleza?"` → chama `get_weather`
- `"Converta 100 dólares em reais"` → chama `convert_currency`
- `"Quem descobriu o Brasil?"` → o modelo responde direto, sem ferramenta

---

## Configuração (`application.yml`)

| Propriedade | Env | Padrão | Descrição |
|-------------|-----|--------|-----------|
| `llm.provider` | `LLM_PROVIDER` | `mock` | `mock` ou `anthropic` |
| `llm.max-iterations` | — | `5` | Limite de idas ao modelo por conversa |
| `anthropic.api-key` | `ANTHROPIC_API_KEY` | — | Chave da API (não versione!) |
| `anthropic.model` | `ANTHROPIC_MODEL` | `claude-sonnet-5` | Modelo usado |
| `demo.run-on-startup` | `DEMO_RUN` | `false` | Roda exemplos no console |

---

## Mapa: conceito → código

| Conceito de Function Calling | Onde está no código |
|------------------------------|---------------------|
| Especificação da ferramenta (name/description/input_schema) | `ToolSpec`, montada em cada `Tool.spec()` |
| Bloco `tool_use` do modelo | `ContentBlock.ToolUse` |
| Execução local da função | `ToolRegistry.execute(...)` |
| Bloco `tool_result` de volta | `ContentBlock.ToolResult` |
| O laço até a resposta final | `ChatService.chat(...)` |
| Tradução para o formato de wire da API | `AnthropicLlmClient` |

> Detalhes do formato de tool use (blocos `tool_use`/`tool_result`, header `anthropic-version`) conferidos na documentação oficial da Messages API da Anthropic.
