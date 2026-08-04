package br.uece.functioncalling.web.dto;

import br.uece.functioncalling.service.ChatResult;
import br.uece.functioncalling.service.ToolCallTrace;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resposta do endpoint de chat. Alem do texto final, expoe o rastro de
 * ferramentas chamadas — evidenciando o ciclo de Function Calling.
 */
public record ChatResponse(
        @Schema(description = "Resposta final do agente") String answer,
        @Schema(description = "Ferramentas invocadas durante o ciclo de Function Calling") List<ToolCallTrace> toolCalls,
        @Schema(description = "Numero de idas ao modelo ate a resposta final") int iterations) {

    public static ChatResponse from(ChatResult result) {
        return new ChatResponse(result.answer(), result.toolCalls(), result.iterations());
    }
}
