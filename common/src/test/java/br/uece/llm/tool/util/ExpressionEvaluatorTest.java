package br.uece.llm.tool.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpressionEvaluatorTest {

    @Test
    void avaliaPrecedenciaEParenteses() {
        assertEquals(57.0, ExpressionEvaluator.evaluate("(12 + 7) * 3"));
        assertEquals(14.0, ExpressionEvaluator.evaluate("2 + 3 * 4"));
        assertEquals(-5.0, ExpressionEvaluator.evaluate("-10 + 5"));
    }

    @Test
    void divisaoPorZeroLancaErro() {
        assertThrows(IllegalArgumentException.class,
                () -> ExpressionEvaluator.evaluate("1 / 0"));
    }

    @Test
    void expressaoInvalidaLancaErro() {
        assertThrows(IllegalArgumentException.class,
                () -> ExpressionEvaluator.evaluate("2 + + "));
    }
}
