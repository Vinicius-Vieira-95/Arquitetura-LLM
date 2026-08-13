package com.br.react_agent.agent;

import java.util.List;

/**
 * Resultado de uma execucao completa do agente ReAct.
 *
 * @param answer     resposta final (o texto apos "Final Answer:")
 * @param steps      sequencia de passos Thought/Action/Observation ate a resposta
 * @param iterations numero de idas ao modelo ate concluir
 */
public record AgentResult(
        String answer,
        List<AgentStep> steps,
        int iterations) {}
