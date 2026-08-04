package br.uece.functioncalling.web;

import br.uece.functioncalling.service.ChatResult;
import br.uece.functioncalling.service.ChatService;
import br.uece.functioncalling.web.dto.ChatRequest;
import br.uece.functioncalling.web.dto.ChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint HTTP para conversar com o agente de Function Calling. */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "Conversa com o agente de Function Calling")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @Operation(
            summary = "Envia uma mensagem ao agente",
            description = "Executa o ciclo de Function Calling: o modelo pode invocar ferramentas "
                    + "(clima, calculadora, cambio etc.) antes de produzir a resposta final.")
    @ApiResponse(responseCode = "200", description = "Resposta gerada com sucesso")
    @ApiResponse(responseCode = "400", description = "Requisicao invalida")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResult result = chatService.chat(request.message());
        return ResponseEntity.ok(ChatResponse.from(result));
    }
}
