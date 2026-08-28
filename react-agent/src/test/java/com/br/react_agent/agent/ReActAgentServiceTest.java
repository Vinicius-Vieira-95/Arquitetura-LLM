package com.br.react_agent.agent;

import com.br.react_agent.llm.MockLlmClient;
import com.br.react_agent.tool.CalculatorTool;
import com.br.react_agent.tool.CurrencyTool;
import com.br.react_agent.tool.ToolRegistry;
import com.br.react_agent.tool.WeatherTool;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o ciclo completo ReAct usando o MockLlmClient, sem Spring:
 * pergunta -> Thought/Action/Action Input -> execucao real da ferramenta ->
 * Observation -> Final Answer. Espelha o ChatServiceTest do projeto irmao
 * function-calling-demo, trocando apenas o formato de interacao com o modelo.
 */
class ReActAgentServiceTest {

    private ReActAgentService agentService;

    @BeforeEach
    void setUp() {
        ToolRegistry registry = new ToolRegistry(
                List.of(new WeatherTool(), new CalculatorTool(), new CurrencyTool()));
        agentService = new ReActAgentService(new MockLlmClient(), registry, new SimpleMeterRegistry(), 5);
    }

    @Test
    void chamaCalculadoraEResponde() {
        AgentResult result = agentService.run("Quanto e' (12 + 7) * 3?");
        assertEquals(1, result.steps().size());
        assertEquals("calculate", result.steps().get(0).action());
        assertFalse(result.steps().get(0).isError());
        assertTrue(result.answer().contains("57"));
        assertEquals(2, result.iterations());
    }

    @Test
    void chamaClimaComCidadeExtraida() {
        AgentResult result = agentService.run("Qual o clima em Fortaleza?");
        assertEquals(1, result.steps().size());
        assertEquals("get_weather", result.steps().get(0).action());
        assertTrue(result.answer().toLowerCase().contains("fortaleza"));
    }

    @Test
    void semFerramentaResponderDireto() {
        AgentResult result = agentService.run("Quem descobriu o Brasil?");
        assertTrue(result.steps().isEmpty());
        assertEquals(1, result.iterations());
    }
}