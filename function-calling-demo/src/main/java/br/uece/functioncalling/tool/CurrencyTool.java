package br.uece.functioncalling.tool;

import br.uece.functioncalling.model.ToolSpec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Conversao de moedas com taxas fixas (simuladas). Demonstra uma ferramenta com
 * multiplos parametros obrigatorios e um enum no schema.
 */
@Component
public class CurrencyTool implements Tool {

    // Taxas fixas em relacao ao BRL (1 unidade da moeda -> X BRL).
    private static final Map<String, Double> EM_BRL = Map.of(
            "BRL", 1.0,
            "USD", 5.40,
            "EUR", 5.85
    );

    @Override
    public String name() {
        return "convert_currency";
    }

    @Override
    public ToolSpec spec() {
        List<String> moedas = List.of("BRL", "USD", "EUR");
        return new ToolSpec(
                name(),
                "Converte um valor entre moedas (BRL, USD, EUR) usando taxas fixas.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "amount", Map.of(
                                        "type", "number",
                                        "description", "Valor a converter"
                                ),
                                "from", Map.of(
                                        "type", "string",
                                        "enum", moedas,
                                        "description", "Moeda de origem"
                                ),
                                "to", Map.of(
                                        "type", "string",
                                        "enum", moedas,
                                        "description", "Moeda de destino"
                                )
                        ),
                        "required", List.of("amount", "from", "to")
                )
        );
    }

    @Override
    public String execute(Map<String, Object> input) {
        double amount = toDouble(input.get("amount"));
        String from = String.valueOf(input.getOrDefault("from", "")).toUpperCase().trim();
        String to = String.valueOf(input.getOrDefault("to", "")).toUpperCase().trim();

        Double fromRate = EM_BRL.get(from);
        Double toRate = EM_BRL.get(to);
        if (fromRate == null || toRate == null) {
            throw new IllegalArgumentException("Moeda nao suportada. Use BRL, USD ou EUR.");
        }
        double converted = amount * fromRate / toRate;
        return "%.2f %s = %.2f %s".formatted(amount, from, converted, to);
    }

    private double toDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parametro 'amount' invalido: " + value);
        }
    }
}
