package com.br.react_agent.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Corpo da requisicao do endpoint do agente. */
public record AgentRequest(
        @Schema(description = "Mensagem/pergunta do usuario para o agente", example = "Qual o clima em Fortaleza?")
        String message) {}
