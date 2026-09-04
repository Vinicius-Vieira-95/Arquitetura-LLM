package com.example.ragreact.agent;

import com.example.ragdemo.store.ScoredChunk;

import java.util.List;

/**
 * Resultado de uma execucao completa do agente RAG + ReAct.
 *
 * @param answer           resposta final (o texto apos "Final Answer:")
 * @param steps            sequencia de passos Thought/Action/Observation ate a resposta
 * @param iterations       numero de idas ao modelo ate concluir
 * @param initialRetrieval chunks recuperados via RAG ANTES do ciclo comecar, injetados
 *                         na Question inicial (o agente pode ter recuperado mais via a
 *                         Action search_documents; esses ficam so no rastro de steps)
 */
public record RagReActResult(
        String answer,
        List<AgentStep> steps,
        int iterations,
        List<ScoredChunk> initialRetrieval) {}
