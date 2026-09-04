package com.example.ragfc.web;

import br.uece.llm.tool.ToolCallTrace;
import com.example.ragfc.service.RagFcResult;
import com.example.ragfc.service.RagFunctionCallingService;
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

/** Testa a camada web isoladamente (RagFunctionCallingService mockado). */
@WebMvcTest(RagFcController.class)
class RagFcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagFunctionCallingService ragFcService;

    @Test
    void respondeComOResultadoDoService() throws Exception {
        RagFcResult result = new RagFcResult(
                "Resposta com base no contexto.",
                List.of(new ToolCallTrace("search_documents", Map.of("query", "isencao"), "[1] ...", false)),
                2,
                List.of());
        when(ragFcService.chat(anyString())).thenReturn(result);

        mockMvc.perform(post("/api/rag-fc/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("message", "Quais documentos sao aceitos?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(result.answer()))
                .andExpect(jsonPath("$.iterations").value(2))
                .andExpect(jsonPath("$.toolCalls[0].tool").value("search_documents"));
    }

    @Test
    void mensagemEmBrancoRetorna400() throws Exception {
        mockMvc.perform(post("/api/rag-fc/chat")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("message", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
