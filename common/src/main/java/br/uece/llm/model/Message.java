package br.uece.llm.model;

import java.util.List;

/**
 * Uma mensagem da conversa: um papel e a lista de blocos de conteudo.
 * O historico completo (incluindo os blocos tool_use e tool_result) precisa ser
 * reenviado a cada chamada, pois o modelo nao mantem estado entre requisicoes.
 */
public record Message(Role role, List<ContentBlock> content) {

    public static Message user(String text) {
        return new Message(Role.USER, List.of(new ContentBlock.Text(text)));
    }

    public static Message assistant(List<ContentBlock> content) {
        return new Message(Role.ASSISTANT, content);
    }

    public static Message toolResults(List<ContentBlock> results) {
        // Resultados de ferramenta viajam sempre numa mensagem de papel USER.
        return new Message(Role.USER, results);
    }
}
