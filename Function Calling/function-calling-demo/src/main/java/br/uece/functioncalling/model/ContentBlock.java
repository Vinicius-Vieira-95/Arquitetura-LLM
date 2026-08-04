package br.uece.functioncalling.model;

import java.util.Map;

/**
 * Bloco de conteudo de uma mensagem. A Messages API modela o conteudo como uma
 * lista de blocos heterogeneos; aqui usamos uma sealed interface para representar
 * os tres tipos relevantes ao ciclo de Function Calling:
 *
 *  - {@link Text}       texto livre (pergunta do usuario ou resposta final do modelo);
 *  - {@link ToolUse}    o modelo decidiu chamar uma ferramenta e produziu os argumentos;
 *  - {@link ToolResult} a aplicacao devolve ao modelo o resultado da execucao da ferramenta.
 */
public sealed interface ContentBlock
        permits ContentBlock.Text, ContentBlock.ToolUse, ContentBlock.ToolResult {

    /** Texto puro. */
    record Text(String text) implements ContentBlock {}

    /**
     * Pedido de chamada de ferramenta emitido pelo modelo.
     *
     * @param id    identificador unico do uso (ex.: "toolu_..."); usado para casar com o resultado
     * @param name  nome da ferramenta que o modelo quer executar
     * @param input argumentos ja estruturados, validados contra o input_schema da ferramenta
     */
    record ToolUse(String id, String name, Map<String, Object> input) implements ContentBlock {}

    /**
     * Resultado da execucao de uma ferramenta, devolvido ao modelo.
     *
     * @param toolUseId referencia ao {@link ToolUse#id()} correspondente
     * @param content   saida textual da ferramenta
     * @param isError   indica se a execucao falhou (mapeia para is_error na API)
     */
    record ToolResult(String toolUseId, String content, boolean isError) implements ContentBlock {}
}
