package br.uece.functioncalling.web;

import br.uece.functioncalling.service.ChatResult;
import br.uece.functioncalling.service.ChatService;
import br.uece.functioncalling.service.ToolCallTrace;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a camada web isoladamente (ChatService mockado): serializacao da resposta,
 * validacao de entrada (400 via GlobalExceptionHandler) e o rastro de ferramentas.
 */
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @Test
    void respondeComOResultadoDoChatService() throws Exception {
        ChatResult result = new ChatResult(
                "Pronto! Resultado das ferramentas:\n- (12 + 7) * 3 = 57",
                List.of(new ToolCallTrace("calculate", Map.of("expression", "(12 + 7) * 3"), "(12 + 7) * 3 = 57", false)),
                2);
        when(chatService.chat(anyString())).thenReturn(result);

        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("message", "Quanto e' (12 + 7) * 3?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(result.answer()))
                .andExpect(jsonPath("$.iterations").value(2))
                .andExpect(jsonPath("$.toolCalls[0].tool").value("calculate"));
    }

    @Test
    void mensagemEmBrancoRetorna400() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("message", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}