package br.uece.functioncalling;

import br.uece.functioncalling.llm.MockLlmClient;
import br.uece.functioncalling.service.ChatResult;
import br.uece.functioncalling.service.ChatService;
import br.uece.functioncalling.tool.CalculatorTool;
import br.uece.functioncalling.tool.CurrencyTool;
import br.uece.functioncalling.tool.ToolRegistry;
import br.uece.functioncalling.tool.WeatherTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o ciclo completo de Function Calling usando o MockLlmClient, sem Spring:
 * pergunta -> tool_use -> execucao -> tool_result -> resposta final.
 */
class ChatServiceTest {

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        ToolRegistry registry = new ToolRegistry(
                List.of(new WeatherTool(), new CalculatorTool(), new CurrencyTool()));
        chatService = new ChatService(new MockLlmClient(), registry, 5);
    }

    @Test
    void chamaCalculadoraEResponde() {
        ChatResult result = chatService.chat("Quanto e' (12 + 7) * 3?");
        assertEquals(1, result.toolCalls().size());
        assertEquals("calculate", result.toolCalls().get(0).tool());
        assertFalse(result.toolCalls().get(0).isError());
        assertTrue(result.answer().contains("57"));
        assertEquals(2, result.iterations());
    }

    @Test
    void chamaClimaComCidadeExtraida() {
        ChatResult result = chatService.chat("Qual o clima em Fortaleza?");
        assertEquals(1, result.toolCalls().size());
        assertEquals("get_weather", result.toolCalls().get(0).tool());
        assertTrue(result.answer().toLowerCase().contains("fortaleza"));
    }

    @Test
    void semFerramentaResponderDireto() {
        ChatResult result = chatService.chat("Quem descobriu o Brasil?");
        assertTrue(result.toolCalls().isEmpty());
        assertEquals(1, result.iterations());
    }
}
