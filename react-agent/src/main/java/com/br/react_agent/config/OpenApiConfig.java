package com.br.react_agent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadados da documentacao Swagger/OpenAPI, disponivel em /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI reactAgentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("React Agent Demo API")
                        .description("Demonstracao dos principios do padrao ReAct (Reasoning + Acting) "
                                + "em Spring Boot (Anthropic real + mock).")
                        .version("1.0.0")
                        .contact(new Contact().name("UECE")));
    }
}
