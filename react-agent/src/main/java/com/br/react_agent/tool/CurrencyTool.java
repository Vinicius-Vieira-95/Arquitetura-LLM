package com.br.react_agent.tool;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conversao de moedas com taxas fixas (simuladas). Como o Action Input e' texto
 * livre (nao ha JSON schema em ReAct), a ferramenta anuncia no proprio
 * {@link #description()} o formato que sabe interpretar e o parseia via regex.
 */
@Component
public class CurrencyTool implements Tool {

    private static final Map<String, Double> EM_BRL = Map.of(
            "BRL", 1.0,
            "USD", 5.40,
            "EUR", 5.85
    );

    private static final Pattern FORMATO = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*([A-Za-z]{3})\\s*(?:to|para|->)?\\s*([A-Za-z]{3})",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String name() {
        return "convert_currency";
    }

    @Override
    public String description() {
        return "Converte um valor entre moedas (BRL, USD, EUR) usando taxas fixas. Action Input no "
                + "formato '<valor> <moeda_origem> to <moeda_destino>', ex.: '100 USD to BRL'.";
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

        Double fromRate = EM_BRL.get(from);
        Double toRate = EM_BRL.get(to);
        if (fromRate == null || toRate == null) {
            throw new IllegalArgumentException("Moeda nao suportada. Use BRL, USD ou EUR.");
        }
        double converted = amount * fromRate / toRate;
        return "%.2f %s = %.2f %s".formatted(amount, from, converted, to);
    }
}
