package com.example.ragreact.tool;

import com.example.ragreact.tool.util.ExpressionEvaluator;
import org.springframework.stereotype.Component;

/**
 * Ferramenta de calculo aritmetico. Demonstra o caso em que o modelo delega
 * a uma ferramenta uma tarefa que ele nao deve fazer "de cabeca" (aritmetica exata).
 */
@Component
public class CalculatorTool implements Tool {

    @Override
    public String name() {
        return "calculate";
    }

    @Override
    public String description() {
        return "Avalia uma expressao aritmetica e retorna o resultado numerico. Suporta +, -, *, / e "
                + "parenteses. Action Input: a propria expressao, ex.: '(12 + 7) * 3'.";
    }

    @Override
    public String execute(String input) {
        String expression = input == null ? "" : input.trim();
        double result = ExpressionEvaluator.evaluate(expression);
        String formatted = (result == Math.rint(result) && !Double.isInfinite(result))
                ? String.valueOf((long) result)
                : String.valueOf(result);
        return "%s = %s".formatted(expression, formatted);
    }
}
