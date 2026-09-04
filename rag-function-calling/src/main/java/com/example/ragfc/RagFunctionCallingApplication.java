package com.example.ragfc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicacao Spring Boot.
 *
 * {@code scanBasePackages} inclui, alem do proprio pacote:
 *  - {@code br.uece.llm}                (common: ToolRegistry, ferramentas, AnthropicLlmClient)
 *  - {@code com.example.ragdemo.embedding} (Embedder/TfidfEmbedder)
 *  - {@code com.example.ragdemo.store}     (InMemoryVectorStore)
 *  - {@code com.example.ragdemo.ingest}    (Chunker/CorpusIndexer)
 *
 * Deliberadamente NAO inclui {@code com.example.ragdemo} (a raiz) nem os subpacotes
 * {@code .web}, {@code .rag} e {@code .llm} de rag-spring-demo: eles trazem
 * RagController, RagService, AnthropicClient e outro OpenApiConfig, que colidiriam
 * com os equivalentes deste modulo (dois beans OpenAPI, dois clientes Anthropic lendo
 * "anthropic.*" com formatos de base-url incompativeis) e exporiam um /ask que nao
 * faz parte desta integracao.
 */
@SpringBootApplication(scanBasePackages = {
        "com.example.ragfc",
        "br.uece.llm",
        "com.example.ragdemo.embedding",
        "com.example.ragdemo.store",
        "com.example.ragdemo.ingest"
})
public class RagFunctionCallingApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagFunctionCallingApplication.class, args);
    }
}
