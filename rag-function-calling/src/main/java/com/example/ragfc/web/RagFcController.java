package com.example.ragfc.web;

import com.example.ragfc.service.RagFcResult;
import com.example.ragfc.service.RagFunctionCallingService;
import com.example.ragfc.web.dto.RagFcRequest;
import com.example.ragfc.web.dto.RagFcResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint HTTP para a integracao RAG + Function Calling. */
@RestController
@RequestMapping("/api/rag-fc")
@Tag(name = "RAG + Function Calling", description = "Contexto recuperado via RAG, com search_documents disponivel como ferramenta")
public class RagFcController {

    private final RagFunctionCallingService ragFcService;

    public RagFcController(RagFunctionCallingService ragFcService) {
        this.ragFcService = ragFcService;
    }

    @PostMapping("/chat")
    @Operation(
            summary = "Envia uma pergunta para o agente RAG + Function Calling",
            description = "Recupera contexto inicial via RAG e executa o ciclo de Function Calling; "
                    + "o modelo pode chamar search_documents para buscar mais contexto, alem das "
                    + "demais ferramentas (calculadora, clima, cambio).")
    @ApiResponse(responseCode = "200", description = "Resposta gerada com sucesso")
    @ApiResponse(responseCode = "400", description = "Requisicao invalida")
    public ResponseEntity<RagFcResponse> chat(@Valid @RequestBody RagFcRequest request) {
        RagFcResult result = ragFcService.chat(request.message());
        return ResponseEntity.ok(RagFcResponse.from(result));
    }
}
