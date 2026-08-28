package com.br.react_agent.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o registro de ferramentas isoladamente: descricao publicada no prompt
 * e o contrato de execute() (sucesso, ferramenta inexistente, ferramenta que lanca excecao).
 * Diferenca em relacao ao registry do function-calling-demo: aqui a entrada e' texto
 * livre (String), nao um Map estruturado.
 */
class ToolRegistryTest {

    private ToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ToolRegistry(List.of(new CalculatorTool(), new CurrencyTool()));
    }

    @Test
    void namesListJuntaOsNomesNaOrdemDeRegistro() {
        assertEquals("calculate, convert_currency", registry.namesList());
    }

    @Test
    void describeAllPublicaNomeEDescricaoDeCadaFerramenta() {
        String described = registry.describeAll();
        assertTrue(described.contains("- calculate:"));
        assertTrue(described.contains("- convert_currency:"));
    }

    @Test
    void hasIndicaSeFerramentaExiste() {
        assertTrue(registry.has("calculate"));
        assertFalse(registry.has("ferramenta_inexistente"));
    }

    @Test
    void executaFerramentaValidaComSucesso() {
        ToolRegistry.Execution exec = registry.execute("calculate", "2 + 2");
        assertFalse(exec.isError());
        assertTrue(exec.output().contains("4"));
    }

    @Test
    void executaFerramentaInexistenteRetornaErro() {
        ToolRegistry.Execution exec = registry.execute("nao_existe", "qualquer coisa");
        assertTrue(exec.isError());
        assertTrue(exec.output().contains("nao encontrada"));
    }

    @Test
    void excecaoNaExecucaoViraSaidaDeErroSemPropagar() {
        ToolRegistry.Execution exec = registry.execute("calculate", "1 / 0");
        assertTrue(exec.isError());
        assertTrue(exec.output().contains("Erro ao executar"));
    }
}