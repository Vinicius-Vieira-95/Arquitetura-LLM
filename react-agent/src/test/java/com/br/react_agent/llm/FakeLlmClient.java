package com.br.react_agent.llm;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dublê determinístico de {@link LlmClient} usado apenas em testes unitários do
 * ciclo ReAct ({@code ReActAgentServiceTest}), para exercitar o laço completo
 * (Thought -> Action -> Action Input -> Observation -> Final Answer) sem depender
 * de rede ou de uma ANTHROPIC_API_KEY. Não é usado em produção: lá o único
 * {@link LlmClient} é o {@link AnthropicLlmClient}.
 */
public class FakeLlmClient implements LlmClient {

    private static final Pattern CIDADE =
            Pattern.compile("(?:em|de|no|na|para)\\s+([\\p{L} ]{3,}?)(?:\\?|$|\\.|,)",
                    Pattern.UNICODE_CHARACTER_CLASS);

    @Override
    public String complete(String prompt, List<String> stopSequences) {
        String question = extractLast(prompt, "Question:", "\nThought:");
        int questionIdx = prompt.lastIndexOf("Question:");
        String transcript = questionIdx >= 0 ? prompt.substring(questionIdx) : prompt;
        int observations = countOccurrences(transcript, "\nObservation:");

        if (observations == 0) {
            return buildActionStep(question);
        }
        String lastObservation = extractLast(prompt, "Observation:", "\nThought:");
        return " Agora tenho a informacao necessaria para responder.\nFinal Answer: " + lastObservation;
    }

    private String buildActionStep(String question) {
        String lower = question.toLowerCase();

        if (lower.matches(".*(clima|tempo|temperatura|graus).*")) {
            String city = extractCity(question).orElse("Fortaleza");
            return " Preciso consultar a ferramenta de clima para responder.\nAction: get_weather\nAction Input: "
                    + city;
        }
        if (lower.matches(".*(calcul|quanto\\s+(?:e|é|da|dá)|resultado|\\d+\\s*[-+*/x]\\s*\\d+).*")) {
            String expr = extractExpression(question);
            if (expr != null) {
                return " Preciso calcular essa expressao.\nAction: calculate\nAction Input: " + expr;
            }
        }
        return " Nao preciso de nenhuma ferramenta para responder isso.\nFinal Answer: "
                + "Nao tenho uma ferramenta apropriada para isso.";
    }

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

    private Optional<String> extractCity(String text) {
        Matcher m = CIDADE.matcher(text);
        return m.find() ? Optional.of(capitalize(m.group(1).trim())) : Optional.empty();
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
