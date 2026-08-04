package br.uece.functioncalling.web;

import br.uece.functioncalling.service.ChatResult;
import br.uece.functioncalling.service.ChatService;
import br.uece.functioncalling.web.dto.ChatRequest;
import br.uece.functioncalling.web.dto.ChatResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint HTTP para conversar com o agente de Function Calling. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResult result = chatService.chat(request.message());
        return ResponseEntity.ok(ChatResponse.from(result));
    }
}
