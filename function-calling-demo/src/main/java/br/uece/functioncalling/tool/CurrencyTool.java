package br.uece.functioncalling.tool;

import br.uece.functioncalling.model.ToolSpec;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Conversao de moedas com taxas reais, via API publica Frankfurter
 * (https://frankfurter.dev, dados do Banco Central Europeu), sem necessidade
 * de chave de API. Demonstra uma ferramenta com multiplos parametros
 * obrigatorios e um enum no schema.
 */
@Component
public class CurrencyTool implements Tool {

    private static final List<String> MOEDAS = List.of("BRL", "USD", "EUR");

    private final RestClient client = RestClient.create("https://api.frankfurter.dev");

    @Override
    public String name() {
        return "convert_currency";
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(
                name(),
                "Converte um valor entre moedas (BRL, USD, EUR) usando taxas de cambio atuais, "
                        + "consultando a API publica Frankfurter (dados do Banco Central Europeu).",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "amount", Map.of(
                                        "type", "number",
                                        "description", "Valor a converter"
                                ),
                                "from", Map.of(
                                        "type", "string",
                                        "enum", MOEDAS,
                                        "description", "Moeda de origem"
                                ),
                                "to", Map.of(
                                        "type", "string",
                                        "enum", MOEDAS,
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

        if (!MOEDAS.contains(from) || !MOEDAS.contains(to)) {
            throw new IllegalArgumentException("Moeda nao suportada. Use BRL, USD ou EUR.");
        }

        double converted = from.equals(to) ? amount : amount * fetchRate(from, to);
        return "%.2f %s = %.2f %s".formatted(amount, from, converted, to);
    }

    private double fetchRate(String from, String to) {
        JsonNode response;
        try {
            response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/latest")
                            .queryParam("base", from)
                            .queryParam("symbols", to)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao consultar taxas de cambio da Frankfurter: " + e.getMessage(), e);
        }

        JsonNode rate = response == null ? null : response.path("rates").path(to);
        if (rate == null || rate.isMissingNode()) {
            throw new IllegalStateException("Resposta invalida da API de cambio da Frankfurter.");
        }
        return rate.asDouble();
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
