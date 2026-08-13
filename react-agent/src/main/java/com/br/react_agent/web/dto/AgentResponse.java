package com.br.react_agent.web.dto;

import com.br.react_agent.agent.AgentResult;
import com.br.react_agent.agent.AgentStep;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resposta do endpoint do agente. Alem da resposta final, expoe o rastro
 * Thought/Action/Observation — evidenciando o ciclo ReAct.
 */
public record AgentResponse(
        @Schema(description = "Resposta final do agente (o texto apos 'Final Answer:')") String answer,
        @Schema(description = "Rastro Thought/Action/Action Input/Observation percorrido pelo agente") List<AgentStep> steps,
        @Schema(description = "Numero de idas ao modelo ate a resposta final") int iterations) {

    public static AgentResponse from(AgentResult result) {
        return new AgentResponse(result.answer(), result.steps(), result.iterations());
    }
}
