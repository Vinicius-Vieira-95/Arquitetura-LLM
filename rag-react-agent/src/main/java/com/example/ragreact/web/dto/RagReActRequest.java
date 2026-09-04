package com.example.ragreact.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Corpo da requisicao do endpoint do agente. */
public record RagReActRequest(
        @Schema(description = "Mensagem/pergunta do usuario para o agente", example = "Quais documentos sao aceitos para isencao?")
        String message) {}
