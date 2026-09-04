package com.example.ragfc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadados da documentacao Swagger/OpenAPI, disponivel em /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ragFunctionCallingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RAG + Function Calling API")
                        .description("Integracao de Retrieval-Augmented Generation com Function Calling: "
                                + "contexto inicial recuperado via RAG, com search_documents disponivel "
                                + "como ferramenta para o modelo buscar mais contexto durante o ciclo.")
                        .version("0.0.1")
                        .contact(new Contact().name("UECE")));
    }
}
