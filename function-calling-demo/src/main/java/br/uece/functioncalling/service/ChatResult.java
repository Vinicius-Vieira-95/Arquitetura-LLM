package br.uece.functioncalling.service;

import java.util.List;

/**
 * Resultado de uma conversa completa.
 *
 * @param answer     texto final produzido pelo modelo
 * @param toolCalls  sequencia de ferramentas chamadas ate chegar a resposta
 * @param iterations numero de idas ao modelo (turnos) ate concluir
 */
public record ChatResult(
        String answer,
        List<ToolCallTrace> toolCalls,
        int iterations) {}
