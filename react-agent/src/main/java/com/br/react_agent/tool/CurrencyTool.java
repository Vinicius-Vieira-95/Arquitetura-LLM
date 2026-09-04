package com.br.react_agent.tool;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conversao de moedas com dados reais, via API publica Frankfurter
 * (https://frankfurter.dev, taxas do Banco Central Europeu), sem necessidade de
 * chave de API. Como o Action Input e' texto livre (nao ha JSON schema em ReAct),
 * a ferramenta anuncia no proprio {@link #description()} o formato que sabe
 * interpretar e o parseia via regex.
 */
@Component
public class CurrencyTool implements Tool {

    private final RestClient restClient = RestClient.create("https://api.frankfurter.dev");

    private static final Pattern FORMATO = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*([A-Za-z]{3})\\s*(?:to|para|->)?\\s*([A-Za-z]{3})",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String name() {
        return "convert_currency";
    }

    @Override
    public String description() {
        return "Converte um valor entre moedas usando taxas de cambio atuais (API Frankfurter/BCE). "
                + "Action Input no formato '<valor> <moeda_origem> to <moeda_destino>', ex.: '100 USD to BRL'.";
    }

    @Override
    public String execute(String input) {
        Matcher m = FORMATO.matcher(input == null ? "" : input.trim());
        if (!m.find()) {
            throw new IllegalArgumentException(
                    "Formato invalido. Use '<valor> <moeda_origem> to <moeda_destino>', ex.: '100 USD to BRL'.");
        }
        double amount = Double.parseDouble(m.group(1).replace(',', '.'));
        String from = m.group(2).toUpperCase();
        String to = m.group(3).toUpperCase();

        if (from.equals(to)) {
            return "%.2f %s = %.2f %s".formatted(amount, from, amount, to);
        }

        double rate = fetchRate(from, to);
        double converted = amount * rate;
        return "%.2f %s = %.2f %s".formatted(amount, from, converted, to);
    }

    private double fetchRate(String from, String to) {
        JsonNode response;
        try {
            response = restClient.get()
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

        JsonNode rateNode = response == null ? null : response.path("rates").path(to);
        if (rateNode == null || rateNode.isMissingNode()) {
            throw new IllegalArgumentException("Moeda nao suportada: " + from + " ou " + to + ".");
        }
        return rateNode.asDouble();
    }
}
