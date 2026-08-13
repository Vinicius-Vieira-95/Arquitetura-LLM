package com.example.ragdemo.rag;

import com.example.ragdemo.store.ScoredChunk;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resposta do RAG: o texto gerado pelo modelo + os chunks que foram recuperados e
 * injetados no prompt (uteis para depurar e para citar fontes).
 *
 * @param question  a pergunta original
 * @param answer    a resposta gerada
 * @param retrieved os chunks usados como contexto, com seus scores
 */
public record RagAnswer(
        @Schema(description = "Pergunta original", example = "Para que serve o Actuator?")
        String question,

        @Schema(description = "Resposta gerada pelo modelo, ancorada nos chunks recuperados")
        String answer,

        @Schema(description = "Chunks usados como contexto, com seus scores de similaridade")
        List<ScoredChunk> retrieved) {
}
