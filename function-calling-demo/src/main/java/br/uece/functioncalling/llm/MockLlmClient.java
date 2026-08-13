package br.uece.functioncalling.llm;

import br.uece.functioncalling.model.ContentBlock;
import br.uece.functioncalling.model.Message;
import br.uece.functioncalling.model.Role;
import br.uece.functioncalling.model.ToolSpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementacao deterministica do modelo, sem chamadas externas. Simula o
 * "raciocinio" de decidir quando chamar uma ferramenta usando regras simples de
 * palavra-chave e regex. Serve para rodar e testar o ciclo completo de Function
 * Calling sem chave de API.
 *
 * Nao pretende ser um parser de linguagem natural robusto: e' apenas o suficiente
 * para exercitar o fluxo (pergunta -> tool_use -> tool_result -> resposta final).
 *
 * Ativa quando llm.provider=mock (padrao).
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    private static final Pattern CIDADE =
            Pattern.compile("(?:em|de|no|na|para)\\s+([\\p{L} ]{3,}?)(?:\\?|$|\\.|,)",
                    Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern VALOR =
            Pattern.compile("\\d+(?:[.,]\\d+)?");

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public Message complete(List<Message> history, List<ToolSpec> tools) {
        Message last = history.get(history.size() - 1);

        // Se a ultima mensagem traz resultados de ferramenta, produz a resposta final.
        if (last.role() == Role.USER && hasToolResult(last)) {
            return Message.assistant(List.of(new ContentBlock.Text(buildFinalAnswer(last))));
        }

        String userText = extractText(last);
        Optional<ContentBlock.ToolUse> call = route(userText.toLowerCase(), userText);

        return call
                .<Message>map(toolUse -> Message.assistant(List.of(toolUse)))
                .orElseGet(() -> Message.assistant(List.of(new ContentBlock.Text(
                        "Nao tenho uma ferramenta apropriada para isso. "
                                + "Posso ajudar com clima, calculos ou conversao de moedas."))));
    }

    /** Decide qual ferramenta chamar (se alguma) a partir do texto do usuario. */
    private Optional<ContentBlock.ToolUse> route(String lower, String original) {
        if (lower.matches(".*(clima|tempo|temperatura|graus).*")) {
            String city = extractCity(original).orElse("Fortaleza");
            return Optional.of(toolUse("get_weather", Map.of("city", city)));
        }
        if (lower.matches(".*(converter|converta|c[aâ]mbio|d[oó]lar|d[oó]lares|euro|euros|reais).*")) {
            return Optional.of(buildCurrencyCall(lower));
        }
        if (lower.matches(".*(calcul|quanto\\s+(?:e|é|da|dá)|resultado|\\d+\\s*[-+*/x]\\s*\\d+).*")) {
            String expr = extractExpression(original);
            if (expr != null) {
                return Optional.of(toolUse("calculate", Map.of("expression", expr)));
            }
        }
        return Optional.empty();
    }

    /**
     * Extrai uma expressao aritmetica do texto mantendo apenas caracteres validos
     * (digitos, operadores, parenteses, ponto). Converte 'x' em '*'.
     * Devolve null se nao houver ao menos um digito e um operador.
     */
    private String extractExpression(String text) {
        String cleaned = text.replaceAll("[^0-9+\\-*/xX(). ]", " ")
                .replace('x', '*').replace('X', '*')
                .trim()
                .replaceAll("\\s+", " ");
        boolean temDigito = cleaned.matches(".*\\d.*");
        boolean temOperador = cleaned.matches(".*[-+*/].*");
        return (temDigito && temOperador) ? cleaned : null;
    }

    /** Monta a chamada de conversao, inferindo origem/destino pela ordem de mencao. */
    private ContentBlock.ToolUse buildCurrencyCall(String lower) {
        double amount = firstNumber(lower).orElse(1.0);
        List<String> ordered = currenciesInOrder(lower);
        String from = ordered.isEmpty() ? "USD" : ordered.get(0);
        String to = ordered.size() > 1
                ? ordered.get(1)
                : (from.equals("BRL") ? "USD" : "BRL");
        return toolUse("convert_currency", Map.of("amount", amount, "from", from, "to", to));
    }

    /** Moedas mencionadas, ordenadas pela posicao em que aparecem no texto. */
    private List<String> currenciesInOrder(String lower) {
        Map<String, Integer> firstIndex = new HashMap<>();
        putIfPresent(firstIndex, "BRL", earliest(lower, "reais", "real", "brl"));
        putIfPresent(firstIndex, "USD", earliest(lower, "dolares", "dólares", "dolar", "dólar", "usd"));
        putIfPresent(firstIndex, "EUR", earliest(lower, "euros", "euro", "eur"));
        return firstIndex.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

    private void putIfPresent(Map<String, Integer> map, String code, int index) {
        if (index >= 0) {
            map.put(code, index);
        }
    }

    private int earliest(String text, String... terms) {
        int best = -1;
        for (String term : terms) {
            int i = text.indexOf(term);
            if (i >= 0 && (best == -1 || i < best)) {
                best = i;
            }
        }
        return best;
    }

    private Optional<Double> firstNumber(String text) {
        Matcher m = VALOR.matcher(text);
        return m.find()
                ? Optional.of(Double.parseDouble(m.group().replace(',', '.')))
                : Optional.empty();
    }

    private Optional<String> extractCity(String text) {
        Matcher m = CIDADE.matcher(text);
        return m.find() ? Optional.of(capitalize(m.group(1).trim())) : Optional.empty();
    }

    private ContentBlock.ToolUse toolUse(String name, Map<String, Object> input) {
        return new ContentBlock.ToolUse("mock_" + counter.incrementAndGet(), name, input);
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

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
