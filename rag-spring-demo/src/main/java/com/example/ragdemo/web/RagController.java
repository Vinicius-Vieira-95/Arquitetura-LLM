package com.example.ragdemo.web;

import com.example.ragdemo.rag.RagAnswer;
import com.example.ragdemo.rag.RagService;
import com.example.ragdemo.store.InMemoryVectorStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "RAG", description = "Perguntas e status do pipeline de recuperacao e geracao")
public class RagController {

    private final RagService ragService;
    private final InMemoryVectorStore vectorStore;

    public RagController(RagService ragService, InMemoryVectorStore vectorStore) {
        this.ragService = ragService;
        this.vectorStore = vectorStore;
    }

    @Operation(summary = "Pergunta ao RAG",
            description = "Recupera os chunks mais relevantes do corpus indexado e gera uma resposta ancorada neles.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resposta gerada com sucesso",
                    content = @Content(schema = @Schema(implementation = RagAnswer.class))),
            @ApiResponse(responseCode = "500", description = "Falha ao chamar o modelo ou corpus nao indexado")
    })
    @PostMapping("/ask")
    public RagAnswer ask(@RequestBody AskRequest request) {
        return ragService.ask(request.question(), request.topK());
    }

    @Operation(summary = "Status simples: quantos chunks estao indexados")
    @ApiResponse(responseCode = "200", description = "Aplicacao ativa",
            content = @Content(examples = @ExampleObject(value = "{\"status\": \"ok\", \"indexedChunks\": 42}")))
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "indexedChunks", vectorStore.size());
    }
}
