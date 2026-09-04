package com.br.react_agent.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente real da Messages API da Anthropic usado como "motor de texto" do ciclo ReAct.
 *
 * A Messages API e' orientada a chat (papeis user/assistant) e nao aceita mais
 * prefill no ultimo turno do assistant nos modelos atuais. Por isso o prompt
 * inteiro (instrucoes + transcricao ReAct acumulada) e' enviado como uma unica
 * mensagem "user", com instrucoes explicitas para o modelo responder apenas com
 * a continuacao do texto — como se estivesse preenchendo o proximo trecho da
 * transcricao a partir do ponto em que ela para.
 *
 * Unica implementacao de {@link LlmClient}: requer ANTHROPIC_API_KEY configurada.
 */
@Component
public class AnthropicLlmClient implements LlmClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public AnthropicLlmClient(
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${anthropic.model:claude-sonnet-5}") String model,
            @Value("${anthropic.max-tokens:1024}") int maxTokens,
            @Value("${anthropic.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${anthropic.version:2023-06-01}") String version) {

        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("anthropic-version", version)
                .defaultHeader("content-type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String complete(String prompt, List<String> stopSequences) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "anthropic.api-key nao configurada. Defina ANTHROPIC_API_KEY ou use llm.provider=mock.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("stop_sequences", stopSequences);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao chamar a Messages API: " + e.getMessage(), e);
        }

        return extractText(response);
    }

    private String extractText(JsonNode response) {
        if (response == null || !response.has("content")) {
            throw new RuntimeException("Resposta da API sem campo 'content'.");
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode block : response.get("content")) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        return text.toString();
    }
}
