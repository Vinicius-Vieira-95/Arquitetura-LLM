package com.example.ragdemo.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corpo da requisicao de pergunta.
 *
 * @param question a pergunta do usuario
 * @param topK     (opcional) quantos chunks recuperar; se nulo, usa o padrao de configuracao
 */
public record AskRequest(
        @Schema(description = "Pergunta em linguagem natural", example = "Para que serve o Actuator?")
        String question,

        @Schema(description = "Quantos chunks recuperar (opcional; usa rag.top-k se omitido)", example = "3")
        Integer topK) {
}
