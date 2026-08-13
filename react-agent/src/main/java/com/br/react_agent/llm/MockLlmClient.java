package com.br.react_agent.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementacao deterministica do modelo, sem chamadas externas. Como o ciclo
 * ReAct nao tem estrutura (e' so texto), o "raciocinio" aqui e' simular a saida
 * que um LLM produziria: na primeira chamada decide uma ferramenta via regras de
 * palavra-chave; a partir da segunda (quando ja existe ao menos uma
 * "Observation:" no prompt) devolve a resposta final com base na ultima
 * observacao.
 *
 * Nao pretende ser um parser de linguagem natural robusto: e' apenas o suficiente
 * para exercitar o laco completo (Thought -> Action -> Action Input -> Observation
 * -> Thought -> Final Answer) sem chave de API.
 *
 * Ativa quando llm.provider=mock (padrao).
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    private static final Pattern CIDADE =
            Pattern.compile("(?:em|de|no|na|para)\\s+([\\p{L} ]{3,}?)(?:\\?|$|\\.|,)",
                    Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern VALOR = Pattern.compile("\\d+(?:[.,]\\d+)?");

    @Override
    public String complete(String prompt, List<String> stopSequences) {
        String question = extractLast(prompt, "Question:", "\nThought:");
        // Conta observacoes apenas na transcricao real (a partir da ultima "Question:"),
        // pois o bloco de instrucoes do prompt tambem menciona "Observation:" ao descrever o formato.
        int questionIdx = prompt.lastIndexOf("Question:");
        String transcript = questionIdx >= 0 ? prompt.substring(questionIdx) : prompt;
        int observations = countOccurrences(transcript, "\nObservation:");

        if (observations == 0) {
            return buildActionStep(question);
        }
        String lastObservation = extractLast(prompt, "Observation:", "\nThought:");
        return " Agora tenho a informacao necessaria para responder.\nFinal Answer: " + lastObservation;
    }

    /** Decide a proxima acao (ou a resposta final direta) a partir da pergunta original. */
    private String buildActionStep(String question) {
        String lower = question.toLowerCase();

        if (lower.matches(".*(clima|tempo|temperatura|graus).*")) {
            String city = extractCity(question).orElse("Fortaleza");
            return " Preciso consultar a ferramenta de clima para responder.\nAction: get_weather\nAction Input: "
                    + city;
        }
        if (lower.matches(".*(converter|converta|c[aâ]mbio|d[oó]lar|d[oó]lares|euro|euros|reais).*")) {
            return " Preciso converter valores entre moedas.\nAction: convert_currency\nAction Input: "
                    + buildCurrencyInput(lower);
        }
        if (lower.matches(".*(calcul|quanto\\s+(?:e|é|da|dá)|resultado|\\d+\\s*[-+*/x]\\s*\\d+).*")) {
            String expr = extractExpression(question);
            if (expr != null) {
                return " Preciso calcular essa expressao.\nAction: calculate\nAction Input: " + expr;
            }
        }
        return " Nao preciso de nenhuma ferramenta para responder isso.\nFinal Answer: "
                + "Nao tenho uma ferramenta apropriada para isso. Posso ajudar com clima, calculos ou "
                + "conversao de moedas.";
    }

    /**
     * Extrai o trecho que segue a ULTIMA ocorrencia de {@code label} ate o proximo
     * {@code stopMarker} (ou o fim da string). Usar a ultima ocorrencia e' necessario
     * porque o proprio template de instrucoes do prompt menciona palavras como
     * "Question:" e "Observation:" ao descrever o formato esperado.
     */
    private String extractLast(String text, String label, String stopMarker) {
        int idx = text.lastIndexOf(label);
        if (idx < 0) {
            return "";
        }
        String rest = text.substring(idx + label.length());
        int stopIdx = rest.indexOf(stopMarker);
        return (stopIdx >= 0 ? rest.substring(0, stopIdx) : rest).trim();
    }

    private int countOccurrences(String text, String marker) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(marker, idx)) >= 0) {
            count++;
            idx += marker.length();
        }
        return count;
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

    /** Monta a Action Input da conversao no formato "<valor> <origem> to <destino>". */
    private String buildCurrencyInput(String lower) {
        double amount = firstNumber(lower).orElse(1.0);
        List<String> ordered = currenciesInOrder(lower);
        String from = ordered.isEmpty() ? "USD" : ordered.get(0);
        String to = ordered.size() > 1
                ? ordered.get(1)
                : (from.equals("BRL") ? "USD" : "BRL");
        String formattedAmount = (amount == Math.rint(amount)) ? String.valueOf((long) amount) : String.valueOf(amount);
        return formattedAmount + " " + from + " to " + to;
    }

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

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
