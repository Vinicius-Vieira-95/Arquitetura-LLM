package com.example.ragfc.support;

import br.uece.llm.llm.LlmClient;
import br.uece.llm.model.ContentBlock;
import br.uece.llm.model.Message;
import br.uece.llm.model.Role;
import br.uece.llm.model.ToolSpec;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Duplo de teste deterministico do {@link LlmClient}, usado apenas nos testes de
 * {@code RagFunctionCallingService} para exercitar o ciclo sem depender da API real.
 *
 * Regras simples de roteamento (sobre o texto da ultima mensagem USER sem tool_result):
 *  - contem "REFINAR:"   -> chama search_documents com o texto apos os dois-pontos
 *  - contem digito+operador -> chama calculate
 *  - caso contrario        -> responde direto com o contexto ja disponivel no prompt
 */
public class FakeLlmClient implements LlmClient {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public Message complete(List<Message> history, List<ToolSpec> tools) {
        Message last = history.get(history.size() - 1);

        if (last.role() == Role.USER && hasToolResult(last)) {
            return Message.assistant(List.of(new ContentBlock.Text(buildFinalAnswer(last))));
        }

        String text = extractText(last);
        Optional<ContentBlock.ToolUse> call = route(text);

        return call
                .<Message>map(toolUse -> Message.assistant(List.of(toolUse)))
                .orElseGet(() -> Message.assistant(List.of(new ContentBlock.Text(
                        "Resposta com base no contexto inicial recuperado."))));
    }

    private Optional<ContentBlock.ToolUse> route(String text) {
        int marker = text.indexOf("REFINAR:");
        if (marker >= 0) {
            String query = text.substring(marker + "REFINAR:".length()).trim();
            return Optional.of(toolUse("search_documents", Map.of("query", query)));
        }
        if (text.matches("(?s).*\\d+\\s*[-+*/]\\s*\\d+.*")) {
            return Optional.of(toolUse("calculate", Map.of("expression", "(12 + 7) * 3")));
        }
        return Optional.empty();
    }

    private ContentBlock.ToolUse toolUse(String name, Map<String, Object> input) {
        return new ContentBlock.ToolUse("fake_" + counter.incrementAndGet(), name, input);
    }

    private String buildFinalAnswer(Message toolResultsMessage) {
        StringBuilder sb = new StringBuilder("Resposta com base na ferramenta:");
        for (ContentBlock block : toolResultsMessage.content()) {
            if (block instanceof ContentBlock.ToolResult tr) {
                sb.append("\n- ").append(tr.content());
            }
        }
        return sb.toString();
    }

    private boolean hasToolResult(Message message) {
        return message.content().stream().anyMatch(b -> b instanceof ContentBlock.ToolResult);
    }

    private String extractText(Message message) {
        return message.content().stream()
                .filter(b -> b instanceof ContentBlock.Text)
                .map(b -> ((ContentBlock.Text) b).text())
                .findFirst()
                .orElse("");
    }
}
