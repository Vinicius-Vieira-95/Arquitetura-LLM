package com.example.ragdemo.store;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Um pedaco ("chunk") do corpus, ja com identificador e origem.
 *
 * @param id     identificador unico (ex.: "spring-boot-basics.md#2")
 * @param source nome do documento de origem
 * @param text   conteudo textual do chunk
 */
public record Chunk(
        @Schema(description = "Identificador unico do chunk", example = "spring-boot-basics.md#2")
        String id,

        @Schema(description = "Nome do documento de origem", example = "spring-boot-basics.md")
        String source,

        @Schema(description = "Conteudo textual do chunk")
        String text) {
}
