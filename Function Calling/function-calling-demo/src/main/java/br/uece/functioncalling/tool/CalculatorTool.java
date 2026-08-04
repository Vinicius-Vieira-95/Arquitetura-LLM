package br.uece.functioncalling.tool;

import br.uece.functioncalling.model.ToolSpec;
import br.uece.functioncalling.tool.util.ExpressionEvaluator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
    public ToolSpec spec() {
        return new ToolSpec(
                name(),
                "Avalia uma expressao aritmetica e retorna o resultado numerico. "
                        + "Suporta +, -, *, / e parenteses.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "expression", Map.of(
                                        "type", "string",
                                        "description", "A expressao a avaliar, ex.: '(12 + 7) * 3'"
                                )
                        ),
                        "required", List.of("expression")
                )
        );
    }

    @Override
    public String execute(Map<String, Object> input) {
        String expression = String.valueOf(input.getOrDefault("expression", "")).trim();
        double result = ExpressionEvaluator.evaluate(expression);
        // Formata inteiros sem casas decimais desnecessarias.
        String formatted = (result == Math.rint(result) && !Double.isInfinite(result))
                ? String.valueOf((long) result)
                : String.valueOf(result);
        return "%s = %s".formatted(expression, formatted);
    }
}
