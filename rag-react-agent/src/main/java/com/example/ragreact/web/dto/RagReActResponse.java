package com.example.ragreact.web.dto;

import com.example.ragdemo.store.ScoredChunk;
import com.example.ragreact.agent.AgentStep;
import com.example.ragreact.agent.RagReActResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resposta do endpoint do agente. Alem da resposta final, expoe o rastro
 * Thought/Action/Observation e os chunks recuperados no RETRIEVE inicial.
 */
public record RagReActResponse(
        @Schema(description = "Resposta final do agente (o texto apos 'Final Answer:')") String answer,
        @Schema(description = "Rastro Thought/Action/Action Input/Observation percorrido pelo agente") List<AgentStep> steps,
        @Schema(description = "Numero de idas ao modelo ate a resposta final") int iterations,
        @Schema(description = "Chunks recuperados via RAG antes do ciclo comecar") List<ScoredChunk> initialRetrieval) {

    public static RagReActResponse from(RagReActResult result) {
        return new RagReActResponse(result.answer(), result.steps(), result.iterations(), result.initialRetrieval());
    }
}
