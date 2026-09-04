package com.example.ragfc.service;

import br.uece.llm.tool.ToolCallTrace;
import com.example.ragdemo.store.ScoredChunk;

import java.util.List;

/**
 * Resultado de uma conversa completa de RAG + Function Calling.
 *
 * @param answer          texto final produzido pelo modelo
 * @param toolCalls       sequencia de ferramentas chamadas durante o ciclo (inclui search_documents)
 * @param iterations      numero de idas ao modelo ate concluir
 * @param initialRetrieval chunks recuperados via RAG ANTES do ciclo comecar, usados para
 *                         montar o contexto inicial (o modelo pode ter recuperado mais via
 *                         search_documents; esses ficam so no rastro de toolCalls)
 */
public record RagFcResult(
        String answer,
        List<ToolCallTrace> toolCalls,
        int iterations,
        List<ScoredChunk> initialRetrieval) {}
