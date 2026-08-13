package br.uece.functioncalling.service;

import java.util.Map;

/**
 * Registro de uma chamada de ferramenta ocorrida durante a conversa.
 * Serve para expor o "caminho" que o modelo percorreu — util para
 * observabilidade e para fins didaticos/experimentais.
 */
public record ToolCallTrace(
        String tool,
        Map<String, Object> input,
        String output,
        boolean isError) {}
