package br.uece.functioncalling.support;

import br.uece.llm.llm.LlmClient;
import br.uece.llm.model.ContentBlock;
import br.uece.llm.model.Message;
import br.uece.llm.model.Role;
import br.uece.llm.model.ToolSpec;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Duplo de teste deterministico do {@link LlmClient}, usado apenas em testes
 * unitarios de {@code ChatService} para exercitar o agentic loop sem depender
 * da API real da Anthropic (que exige chave e tem custo).
 */
public class FakeLlmClient implements LlmClient {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public Message complete(List<Message> history, List<ToolSpec> tools) {
        Message last = history.get(history.size() - 1);

        if (last.role() == Role.USER && hasToolResult(last)) {
            return Message.assistant(List.of(new ContentBlock.Text(buildFinalAnswer(last))));
        }

        String userText = extractText(last).toLowerCase();
        Optional<ContentBlock.ToolUse> call = route(userText, extractText(last));

        return call
                .<Message>map(toolUse -> Message.assistant(List.of(toolUse)))
                .orElseGet(() -> Message.assistant(List.of(new ContentBlock.Text(
                        "Nao tenho uma ferramenta apropriada para isso."))));
    }

    private Optional<ContentBlock.ToolUse> route(String lower, String original) {
        if (lower.matches(".*(clima|tempo|temperatura|graus).*")) {
            return Optional.of(toolUse("get_weather", java.util.Map.of("city", "Fortaleza")));
        }
        if (lower.matches(".*(converter|converta|d[oó]lar|d[oó]lares|euro|euros|reais).*")) {
            return Optional.of(toolUse("convert_currency",
                    java.util.Map.of("amount", 100.0, "from", "USD", "to", "BRL")));
        }
        if (lower.matches(".*\\d+\\s*[-+*/x]\\s*\\d+.*") || lower.contains("calcul")) {
            String expr = extractExpression(original);
            if (expr != null) {
                return Optional.of(toolUse("calculate", java.util.Map.of("expression", expr)));
            }
        }
        return Optional.empty();
    }

    private String extractExpression(String text) {
        String cleaned = text.replaceAll("[^0-9+\\-*/xX(). ]", " ")
                .replace('x', '*').replace('X', '*')
                .trim()
                .replaceAll("\\s+", " ");
        boolean temDigito = cleaned.matches(".*\\d.*");
        boolean temOperador = cleaned.matches(".*[-+*/].*");
        return (temDigito && temOperador) ? cleaned : null;
    }

    private ContentBlock.ToolUse toolUse(String name, java.util.Map<String, Object> input) {
        return new ContentBlock.ToolUse("fake_" + counter.incrementAndGet(), name, input);
    }

    private String buildFinalAnswer(Message toolResultsMessage) {
        StringBuilder sb = new StringBuilder("Pronto! Resultado das ferramentas:");
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
