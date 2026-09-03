package br.uece.functioncalling.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadados da documentacao Swagger/OpenAPI, disponivel em /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI functionCallingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Function Calling Demo API")
                        .description("Demonstracao dos principios de Function Calling em Spring Boot, com chamadas reais a Messages API da Anthropic.")
                        .version("1.0.0")
                        .contact(new Contact().name("UECE")));
    }
}