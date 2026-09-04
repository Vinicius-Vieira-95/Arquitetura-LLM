package com.example.ragreact;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicacao Spring Boot.
 *
 * {@code scanBasePackages} inclui, alem do proprio pacote, apenas os subpacotes
 * necessarios de rag-spring-demo (embedding/store/ingest) — nao a raiz
 * {@code com.example.ragdemo} nem {@code .web}/{@code .rag}/{@code .llm}, que
 * trariam RagController/RagService/AnthropicClient e outro bean OpenAPI, entrando
 * em conflito com os equivalentes deste modulo. Ver rag-function-calling, que usa
 * a mesma estrategia.
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.ragreact",
        "com.example.ragdemo.embedding",
        "com.example.ragdemo.store",
        "com.example.ragdemo.ingest"
})
public class RagReActAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagReActAgentApplication.class, args);
    }
}
