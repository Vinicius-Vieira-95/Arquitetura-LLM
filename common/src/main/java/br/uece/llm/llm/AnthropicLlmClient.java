package br.uece.llm.llm;

import br.uece.llm.model.ContentBlock;
import br.uece.llm.model.Message;
import br.uece.llm.model.Role;
import br.uece.llm.model.ToolSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente real da Messages API da Anthropic com Function Calling.
 *
 * Responsabilidades:
 *  - traduzir o historico interno ({@link Message}) para o formato de wire da API;
 *  - publicar as {@link ToolSpec} no parametro "tools";
 *  - interpretar a resposta (blocos text e tool_use) de volta para o modelo interno.
 */
@Component
public class AnthropicLlmClient implements LlmClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public AnthropicLlmClient(
            ObjectMapper objectMapper,
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${anthropic.model:claude-sonnet-5}") String model,
            @Value("${anthropic.max-tokens:1024}") int maxTokens,
            @Value("${anthropic.base-url:https://api.anthropic.com}") String baseUrl,
            @Value("${anthropic.version:2023-06-01}") String version) {

        this.objectMapper = objectMapper;
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
    public Message complete(List<Message> history, List<ToolSpec> tools) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "anthropic.api-key nao configurada. Defina a variavel de ambiente ANTHROPIC_API_KEY.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("tools", tools.stream().map(this::toolToWire).toList());
        body.put("messages", history.stream().map(this::messageToWire).toList());

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

        return parseResponse(response);
    }

    // ---- Traducao modelo interno -> formato de wire ------------------------

    private Map<String, Object> toolToWire(ToolSpec spec) {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", spec.name());
        tool.put("description", spec.description());
        tool.put("input_schema", spec.inputSchema());
        return tool;
    }

    private Map<String, Object> messageToWire(Message message) {
        Map<String, Object> wire = new LinkedHashMap<>();
        wire.put("role", message.role() == Role.USER ? "user" : "assistant");
        wire.put("content", message.content().stream().map(this::blockToWire).toList());
        return wire;
    }

    private Map<String, Object> blockToWire(ContentBlock block) {
        Map<String, Object> wire = new LinkedHashMap<>();
        if (block instanceof ContentBlock.Text text) {
            wire.put("type", "text");
            wire.put("text", text.text());
        } else if (block instanceof ContentBlock.ToolUse toolUse) {
            wire.put("type", "tool_use");
            wire.put("id", toolUse.id());
            wire.put("name", toolUse.name());
            wire.put("input", toolUse.input());
        } else if (block instanceof ContentBlock.ToolResult toolResult) {
            wire.put("type", "tool_result");
            wire.put("tool_use_id", toolResult.toolUseId());
            wire.put("content", toolResult.content());
            wire.put("is_error", toolResult.isError());
        }
        return wire;
    }

    // ---- Traducao formato de wire -> modelo interno ------------------------

    @SuppressWarnings("unchecked")
    private Message parseResponse(JsonNode response) {
        if (response == null || !response.has("content")) {
            throw new RuntimeException("Resposta da API sem campo 'content'.");
        }
        List<ContentBlock> blocks = new ArrayList<>();
        for (JsonNode node : response.get("content")) {
            String type = node.path("type").asText();
            switch (type) {
                case "text" -> blocks.add(new ContentBlock.Text(node.path("text").asText()));
                case "tool_use" -> {
                    Map<String, Object> input =
                            objectMapper.convertValue(node.path("input"), Map.class);
                    blocks.add(new ContentBlock.ToolUse(
                            node.path("id").asText(),
                            node.path("name").asText(),
                            input == null ? Map.of() : input));
                }
                default -> { /* ignora blocos nao usados neste demo (ex.: thinking) */ }
            }
        }
        return Message.assistant(blocks);
    }
}
