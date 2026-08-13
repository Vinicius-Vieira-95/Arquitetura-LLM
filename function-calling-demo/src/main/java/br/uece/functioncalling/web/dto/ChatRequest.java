package br.uece.functioncalling.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Corpo da requisicao do endpoint de chat. */
public record ChatRequest(
        @NotBlank(message = "message nao pode ser vazio")
        @Schema(description = "Mensagem do usuario para o agente", example = "Qual o clima em Fortaleza?")
        String message) {}
