package br.uece.functioncalling.tool;

import br.uece.functioncalling.model.ToolSpec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Ferramenta de clima (simulada de forma deterministica).
 *
 * Numa aplicacao real, aqui entraria uma chamada a um servico externo de
 * meteorologia. Para o demo, geramos dados estaveis a partir do nome da cidade,
 * de modo que a mesma cidade sempre retorne o mesmo clima.
 */
@Component
public class WeatherTool implements Tool {

    private static final String[] CONDICOES = {
            "ceu limpo", "parcialmente nublado", "nublado", "chuva fraca", "sol forte"
    };

    @Override
    public String name() {
        return "get_weather";
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(
                name(),
                "Retorna as condicoes climaticas atuais de uma cidade.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "city", Map.of(
                                        "type", "string",
                                        "description", "Nome da cidade, ex.: 'Fortaleza'"
                                )
                        ),
                        "required", List.of("city")
                )
        );
    }

    @Override
    public String execute(Map<String, Object> input) {
        String city = String.valueOf(input.getOrDefault("city", "")).trim();
        if (city.isEmpty()) {
            throw new IllegalArgumentException("Parametro 'city' e' obrigatorio.");
        }
        int hash = Math.abs(city.toLowerCase().hashCode());
        int temperatura = 18 + (hash % 17);            // faixa 18..34 C
        String condicao = CONDICOES[hash % CONDICOES.length];
        int umidade = 40 + (hash % 51);                // faixa 40..90 %
        return "Clima em %s: %d C, %s, umidade %d%%.".formatted(city, temperatura, condicao, umidade);
    }
}
