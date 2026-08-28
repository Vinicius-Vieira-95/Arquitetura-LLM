package com.br.react_agent.web;

import com.br.react_agent.agent.AgentResult;
import com.br.react_agent.agent.AgentStep;
import com.br.react_agent.agent.ReActAgentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a camada web isoladamente (ReActAgentService mockado): serializacao da
 * resposta, o rastro Thought/Action/Observation e a validacao manual de mensagem em branco.
 */
@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReActAgentService agentService;

    @Test
    void respondeComOResultadoDoAgentService() throws Exception {
        AgentResult result = new AgentResult(
                "(12 + 7) * 3 = 57",
                List.of(new AgentStep("Preciso calcular essa expressao.", "calculate", "(12 + 7) * 3",
                        "(12 + 7) * 3 = 57", false)),
                2);
        when(agentService.run(anyString())).thenReturn(result);

        mockMvc.perform(post("/api/agent")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("message", "Quanto e' (12 + 7) * 3?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(result.answer()))
                .andExpect(jsonPath("$.iterations").value(2))
                .andExpect(jsonPath("$.steps[0].action").value("calculate"));
    }

    @Test
    void mensagemEmBrancoRetorna400() throws Exception {
        mockMvc.perform(post("/api/agent")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("message", ""))))
                .andExpect(status().isBadRequest());
    }
}