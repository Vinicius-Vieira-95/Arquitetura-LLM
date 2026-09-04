package com.example.ragreact.web;

import com.example.ragreact.agent.AgentStep;
import com.example.ragreact.agent.RagReActAgentService;
import com.example.ragreact.agent.RagReActResult;
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

/** Testa a camada web isoladamente (RagReActAgentService mockado). */
@WebMvcTest(RagReActController.class)
class RagReActControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagReActAgentService agentService;

    @Test
    void respondeComOResultadoDoAgentService() throws Exception {
        RagReActResult result = new RagReActResult(
                "Resposta com base no contexto.",
                List.of(new AgentStep("Preciso buscar mais contexto.", "search_documents", "isencao",
                        "[1] ...", false)),
                2,
                List.of());
        when(agentService.run(anyString())).thenReturn(result);

        mockMvc.perform(post("/api/rag-react")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("message", "Quais documentos sao aceitos?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(result.answer()))
                .andExpect(jsonPath("$.iterations").value(2))
                .andExpect(jsonPath("$.steps[0].action").value("search_documents"));
    }

    @Test
    void mensagemEmBrancoRetorna400() throws Exception {
        mockMvc.perform(post("/api/rag-react")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("message", ""))))
                .andExpect(status().isBadRequest());
    }
}
