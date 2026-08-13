package br.uece.functioncalling.model;

/**
 * Papel de cada mensagem na conversa, conforme a Messages API da Anthropic.
 * O sistema (system prompt) e' enviado em parametro proprio, nao como Role.
 */
public enum Role {
    USER,
    ASSISTANT
}
