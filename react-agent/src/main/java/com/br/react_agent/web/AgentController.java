package com.br.react_agent.web;

import com.br.react_agent.agent.AgentResult;
import com.br.react_agent.agent.ReActAgentService;
import com.br.react_agent.web.dto.AgentRequest;
import com.br.react_agent.web.dto.AgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint HTTP para conversar com o agente ReAct. */
@RestController
@RequestMapping("/api/agent")
@Tag(name = "Agent", description = "Conversa com o agente ReAct (Reasoning + Acting)")
public class AgentController {

    private final ReActAgentService agentService;

    public AgentController(ReActAgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    @Operation(
            summary = "Envia uma mensagem ao agente",
            description = "Executa o ciclo ReAct: o modelo alterna entre Thought/Action/Action Input, a "
                    + "aplicacao executa a ferramenta escolhida e devolve a Observation, ate o modelo "
                    + "produzir a Final Answer.")
    @ApiResponse(responseCode = "200", description = "Resposta gerada com sucesso")
    @ApiResponse(responseCode = "400", description = "Requisicao invalida (mensagem vazia)")
    public ResponseEntity<AgentResponse> run(@RequestBody AgentRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AgentResult result = agentService.run(request.message());
        return ResponseEntity.ok(AgentResponse.from(result));
    }
}
