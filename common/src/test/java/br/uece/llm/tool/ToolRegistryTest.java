package br.uece.llm.tool;

import br.uece.llm.model.ToolSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o registro de ferramentas isoladamente: publicacao de specs e o
 * contrato de execute() (sucesso, ferramenta inexistente, ferramenta que lanca excecao).
 */
class ToolRegistryTest {

    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry(List.of(new CalculatorTool(), new CurrencyTool()));
    }

    @Test
    void publicaSpecsDeTodasAsFerramentas() {
        List<ToolSpec> specs = registry.specs();
        assertEquals(2, specs.size());
        assertTrue(specs.stream().anyMatch(s -> s.name().equals("calculate")));
        assertTrue(specs.stream().anyMatch(s -> s.name().equals("convert_currency")));
    }

    @Test
    void hasIndicaSeFerramentaExiste() {
        assertTrue(registry.has("calculate"));
        assertFalse(registry.has("ferramenta_inexistente"));
    }

    @Test
    void executaFerramentaValidaComSucesso() {
        ToolRegistry.Execution exec = registry.execute("calculate", Map.of("expression", "2 + 2"));
        assertFalse(exec.isError());
        assertTrue(exec.output().contains("4"));
    }

    @Test
    void executaFerramentaInexistenteRetornaErro() {
        ToolRegistry.Execution exec = registry.execute("nao_existe", Map.of());
        assertTrue(exec.isError());
        assertTrue(exec.output().contains("nao encontrada"));
    }

    @Test
    void excecaoNaExecucaoViraSaidaDeErroSemPropagar() {
        ToolRegistry.Execution exec = registry.execute("calculate", Map.of("expression", "1 / 0"));
        assertTrue(exec.isError());
        assertTrue(exec.output().contains("Erro ao executar"));
    }
}
