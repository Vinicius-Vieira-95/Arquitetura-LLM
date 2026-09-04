package com.example.ragfc.web.dto;

import br.uece.llm.tool.ToolCallTrace;
import com.example.ragdemo.store.ScoredChunk;
import com.example.ragfc.service.RagFcResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Resposta do endpoint de RAG + Function Calling. */
public record RagFcResponse(
        @Schema(description = "Resposta final do modelo") String answer,
        @Schema(description = "Ferramentas invocadas durante o ciclo (inclui search_documents)") List<ToolCallTrace> toolCalls,
        @Schema(description = "Numero de idas ao modelo ate a resposta final") int iterations,
        @Schema(description = "Chunks recuperados via RAG antes do ciclo comecar") List<ScoredChunk> initialRetrieval) {

    public static RagFcResponse from(RagFcResult result) {
        return new RagFcResponse(result.answer(), result.toolCalls(), result.iterations(), result.initialRetrieval());
    }
}
