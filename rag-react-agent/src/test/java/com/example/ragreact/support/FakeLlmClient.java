package com.example.ragreact.support;

import com.example.ragreact.llm.LlmClient;

import java.util.List;

/**
 * Duplo de teste deterministico do {@link LlmClient}, usado apenas nos testes de
 * {@code RagReActAgentService} para exercitar o ciclo sem depender da API real.
 *
 * Regras (sobre a regiao "Question:" ate "\nThought:" do prompt, que ja vem com o
 * contexto RAG injetado por buildAugmentedQuestion):
 *  - sem nenhuma Observation ainda:
 *      - contem "REFINAR:"      -> Action: search_documents, Action Input apos os dois-pontos
 *      - contem digito+operador -> Action: calculate
 *      - caso contrario          -> Final Answer direto, citando o contexto inicial
 *  - com Observation na transcricao -> Final Answer usando a ultima Observation
 */
public class FakeLlmClient implements LlmClient {

    @Override
    public String complete(String prompt, List<String> stopSequences) {
        int questionIdx = prompt.lastIndexOf("Question:");
        String transcript = questionIdx >= 0 ? prompt.substring(questionIdx) : prompt;
        int observations = countOccurrences(transcript, "\nObservation:");

        if (observations == 0) {
            String question = extractLast(prompt, "Question:", "\nThought:");
            return buildActionStep(question);
        }
        String lastObservation = extractLast(prompt, "Observation:", "\nThought:");
        return " Agora tenho a informacao necessaria para responder.\nFinal Answer: "
                + "Resposta com base na ferramenta:\n- " + lastObservation;
    }

    private String buildActionStep(String question) {
        int marker = question.indexOf("REFINAR:");
        if (marker >= 0) {
            String query = question.substring(marker + "REFINAR:".length()).trim();
            return " O contexto inicial nao e' suficiente; preciso refinar a busca.\n"
                    + "Action: search_documents\nAction Input: " + query;
        }
        if (question.matches("(?s).*\\d+\\s*[-+*/]\\s*\\d+.*")) {
            return " Preciso calcular essa expressao.\nAction: calculate\nAction Input: (12 + 7) * 3";
        }
        return " O contexto inicial recuperado ja e' suficiente para responder.\nFinal Answer: "
                + "Resposta com base no contexto inicial recuperado.";
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
}
