package com.example.ragreact.web;

import com.example.ragreact.agent.RagReActAgentService;
import com.example.ragreact.agent.RagReActResult;
import com.example.ragreact.web.dto.RagReActRequest;
import com.example.ragreact.web.dto.RagReActResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint HTTP para a integracao RAG + ReAct. */
@RestController
@RequestMapping("/api/rag-react")
@Tag(name = "RAG + ReAct", description = "Contexto recuperado via RAG injetado na Question; search_documents disponivel como Action")
public class RagReActController {

    private final RagReActAgentService agentService;

    public RagReActController(RagReActAgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    @Operation(
            summary = "Envia uma mensagem ao agente RAG + ReAct",
            description = "Recupera contexto inicial via RAG e injeta na Question; executa o ciclo ReAct "
                    + "(Thought/Action/Action Input/Observation) ate a Final Answer. search_documents "
                    + "fica disponivel como Action para o agente refinar a busca.")
    @ApiResponse(responseCode = "200", description = "Resposta gerada com sucesso")
    @ApiResponse(responseCode = "400", description = "Requisicao invalida (mensagem vazia)")
    public ResponseEntity<RagReActResponse> run(@RequestBody RagReActRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        RagReActResult result = agentService.run(request.message());
        return ResponseEntity.ok(RagReActResponse.from(result));
    }
}
