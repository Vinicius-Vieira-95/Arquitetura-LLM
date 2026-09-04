package br.uece.llm.model;

import java.util.Map;

/**
 * Especificacao de uma ferramenta enviada ao modelo no parametro "tools".
 * Corresponde ao trio (name, description, input_schema) da Messages API.
 * O input_schema e' um JSON Schema descrevendo os parametros aceitos.
 */
public record ToolSpec(String name, String description, Map<String, Object> inputSchema) {}
