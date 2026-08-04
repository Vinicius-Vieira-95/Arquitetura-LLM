package com.example.ragdemo.store;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resultado de uma busca: um chunk e seu score de similaridade (cosseno) com a consulta.
 *
 * @param chunk o chunk recuperado
 * @param score similaridade de cosseno (0 a 1, quanto maior mais relevante)
 */
public record ScoredChunk(
        @Schema(description = "Chunk recuperado")
        Chunk chunk,

        @Schema(description = "Similaridade de cosseno com a pergunta (0 a 1, quanto maior mais relevante)", example = "0.82")
        double score) {
}
