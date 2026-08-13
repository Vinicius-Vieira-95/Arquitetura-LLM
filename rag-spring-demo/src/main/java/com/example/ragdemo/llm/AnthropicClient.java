package com.example.ragdemo.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente minimo da Anthropic Messages API (etapa de GERACAO do RAG).
 *
 * <p>Endpoint: POST https://api.anthropic.com/v1/messages
 * <p>Autenticacao: header {@code x-api-key} + {@code anthropic-version: 2023-06-01}.
 *
 * <p>Usa o {@link HttpClient} nativo do JDK e o {@link ObjectMapper} do Jackson
 * (que ja vem com o spring-boot-starter-web), para nao depender de SDK externo.
 */
@Component
public class AnthropicClient {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int maxTokens;
    private final String anthropicVersion;

    public AnthropicClient(
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${anthropic.model:claude-haiku-4-5}") String model,
            @Value("${anthropic.base-url:https://api.anthropic.com/v1/messages}") String baseUrl,
            @Value("${anthropic.max-tokens:1024}") int maxTokens,
            @Value("${anthropic.version:2023-06-01}") String anthropicVersion) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.maxTokens = maxTokens;
        this.anthropicVersion = anthropicVersion;
    }

    public String generate(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "ANTHROPIC_API_KEY nao configurada. Defina a variavel de ambiente antes de subir a app.");
        }

        try {
            String body = buildRequestBody(systemPrompt, userPrompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("content-type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", anthropicVersion)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("Anthropic API retornou HTTP " + response.statusCode()
                        + ": " + response.body());
            }

            return extractText(response.body());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao chamar a Anthropic API: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String systemPrompt, String userPrompt) {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("max_tokens", maxTokens);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            root.put("system", systemPrompt);
        }
        ArrayNode messages = root.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        return root.toString();
    }

    /** A resposta vem em content[]: concatena os blocos do tipo "text". */
    private String extractText(String responseBody) throws Exception {
        JsonNode root = mapper.readTree(responseBody);
        JsonNode content = root.path("content");
        StringBuilder sb = new StringBuilder();
        if (content.isArray()) {
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    sb.append(block.path("text").asText());
                }
            }
        }
        return sb.toString().strip();
    }
}
