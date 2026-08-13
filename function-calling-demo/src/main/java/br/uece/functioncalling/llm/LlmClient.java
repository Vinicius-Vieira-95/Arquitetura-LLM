package br.uece.functioncalling.llm;

import br.uece.functioncalling.model.Message;
import br.uece.functioncalling.model.ToolSpec;

import java.util.List;

/**
 * Abstracao do modelo de linguagem. Recebe o historico e as ferramentas
 * disponiveis e devolve a proxima mensagem do ASSISTANT — que pode conter
 * texto final ou um ou mais blocos tool_use.
 *
 * Duas implementacoes: {@link AnthropicLlmClient} (API real) e
 * {@link MockLlmClient} (deterministica, sem chave). O orquestrador
 * ({@code ChatService}) nao sabe qual esta em uso.
 */
public interface LlmClient {

    Message complete(List<Message> history, List<ToolSpec> tools);
}
