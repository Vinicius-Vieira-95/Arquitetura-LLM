package com.example.ragreact.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadados da documentacao Swagger/OpenAPI, disponivel em /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ragReActOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RAG + ReAct API")
                        .description("Integracao de Retrieval-Augmented Generation com o padrao ReAct: "
                                + "contexto inicial recuperado via RAG e injetado na Question, com "
                                + "search_documents disponivel como Action para o agente refinar a busca.")
                        .version("0.0.1")
                        .contact(new Contact().name("UECE")));
    }
}
