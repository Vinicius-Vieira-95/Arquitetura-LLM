package br.uece.functioncalling.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo da requisicao do endpoint de chat. */
public record ChatRequest(
        @NotBlank(message = "message nao pode ser vazio")
        String message) {}
