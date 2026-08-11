package com.example.ragdemo.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadados exibidos no Swagger UI (/swagger-ui.html) e em /v3/api-docs. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ragOpenApi() {
        return new OpenAPI().info(new Info()
                .title("RAG Spring Demo")
                .description("Mini RAG em Spring Boot para consulta as regras do Edital do Vestibular "
                        + "da UECE: chunking, embeddings, busca por similaridade e geracao via Anthropic API.")
                .version("v0.0.2"));
    }
}
