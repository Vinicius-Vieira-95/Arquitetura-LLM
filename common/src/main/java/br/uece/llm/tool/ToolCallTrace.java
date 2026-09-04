package br.uece.llm.tool;

import java.util.Map;

/**
 * Registro de uma chamada de ferramenta ocorrida durante um ciclo de Function Calling.
 * Serve para expor o "caminho" que o modelo percorreu — util para
 * observabilidade e para fins didaticos/experimentais.
 */
public record ToolCallTrace(
        String tool,
        Map<String, Object> input,
        String output,
        boolean isError) {}
