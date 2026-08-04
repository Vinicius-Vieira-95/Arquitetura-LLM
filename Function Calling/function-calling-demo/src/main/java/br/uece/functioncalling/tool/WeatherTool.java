package br.uece.functioncalling.tool;

import br.uece.functioncalling.model.ToolSpec;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Ferramenta de clima com dados reais, via API publica Open-Meteo
 * (https://open-meteo.com), sem necessidade de chave de API.
 *
 * Executa duas chamadas: geocoding (nome da cidade -> latitude/longitude) e
 * forecast (latitude/longitude -> condicoes atuais).
 */
@Component
public class WeatherTool implements Tool {

    private final RestClient geocodingClient = RestClient.create("https://geocoding-api.open-meteo.com");
    private final RestClient forecastClient = RestClient.create("https://api.open-meteo.com");

    @Override
    public String name() {
        return "get_weather";
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(
                name(),
                "Retorna as condicoes climaticas atuais de uma cidade, consultando a API publica Open-Meteo.",
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

        JsonNode local = geocode(city);
        double lat = local.path("latitude").asDouble();
        double lon = local.path("longitude").asDouble();
        String nomeResolvido = local.path("name").asText(city);
        String pais = local.path("country").asText("");

        JsonNode current = forecast(lat, lon);
        double temperatura = current.path("temperature_2m").asDouble();
        int umidade = current.path("relative_humidity_2m").asInt();
        String condicao = descreverCondicao(current.path("weather_code").asInt());

        return "Clima em %s%s: %.1f C, %s, umidade %d%%.".formatted(
                nomeResolvido, pais.isBlank() ? "" : " (" + pais + ")", temperatura, condicao, umidade);
    }

    private JsonNode geocode(String city) {
        JsonNode response;
        try {
            response = geocodingClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search")
                            .queryParam("name", city)
                            .queryParam("count", 1)
                            .queryParam("language", "pt")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao consultar geocoding do Open-Meteo: " + e.getMessage(), e);
        }

        JsonNode results = response == null ? null : response.path("results");
        if (results == null || !results.isArray() || results.isEmpty()) {
            throw new IllegalArgumentException("Cidade nao encontrada: " + city);
        }
        return results.get(0);
    }

    private JsonNode forecast(double lat, double lon) {
        JsonNode response;
        try {
            response = forecastClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lon)
                            .queryParam("current", "temperature_2m,relative_humidity_2m,weather_code")
                            .queryParam("timezone", "auto")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao consultar previsao do Open-Meteo: " + e.getMessage(), e);
        }

        JsonNode current = response == null ? null : response.path("current");
        if (current == null || current.isMissingNode()) {
            throw new IllegalStateException("Resposta invalida da API de previsao do Open-Meteo.");
        }
        return current;
    }

    // Codigos WMO (weather_code) usados pela Open-Meteo -> descricao em portugues.
    // Referencia: https://open-meteo.com/en/docs (secao WMO Weather interpretation codes)
    private String descreverCondicao(int code) {
        return switch (code) {
            case 0 -> "ceu limpo";
            case 1, 2, 3 -> "parcialmente nublado";
            case 45, 48 -> "neblina";
            case 51, 53, 55 -> "garoa";
            case 56, 57 -> "garoa congelante";
            case 61, 63, 65 -> "chuva";
            case 66, 67 -> "chuva congelante";
            case 71, 73, 75, 77 -> "neve";
            case 80, 81, 82 -> "pancadas de chuva";
            case 85, 86 -> "pancadas de neve";
            case 95 -> "trovoada";
            case 96, 99 -> "trovoada com granizo";
            default -> "condicao desconhecida";
        };
    }
}
