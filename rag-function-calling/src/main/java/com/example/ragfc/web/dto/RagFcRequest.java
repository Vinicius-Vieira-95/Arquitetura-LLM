package com.example.ragfc.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Corpo da requisicao do endpoint de RAG + Function Calling. */
public record RagFcRequest(
        @NotBlank(message = "message nao pode ser vazio")
        @Schema(description = "Pergunta do usuario", example = "Quais documentos sao aceitos para isencao da taxa?")
        String message) {}
