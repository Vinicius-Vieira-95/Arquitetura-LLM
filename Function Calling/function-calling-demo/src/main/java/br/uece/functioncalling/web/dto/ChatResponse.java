package br.uece.functioncalling.web.dto;

import br.uece.functioncalling.service.ChatResult;
import br.uece.functioncalling.service.ToolCallTrace;

import java.util.List;

/**
 * Resposta do endpoint de chat. Alem do texto final, expoe o rastro de
 * ferramentas chamadas — evidenciando o ciclo de Function Calling.
 */
public record ChatResponse(
        String answer,
        List<ToolCallTrace> toolCalls,
        int iterations) {

    public static ChatResponse from(ChatResult result) {
        return new ChatResponse(result.answer(), result.toolCalls(), result.iterations());
    }
}
