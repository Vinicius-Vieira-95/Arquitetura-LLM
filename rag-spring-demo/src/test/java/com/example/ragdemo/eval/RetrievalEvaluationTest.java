package com.example.ragdemo.eval;

import com.example.ragdemo.embedding.Embedder;
import com.example.ragdemo.store.InMemoryVectorStore;
import com.example.ragdemo.store.ScoredChunk;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Avalia a QUALIDADE do retrieval (nao a geracao) contra o corpus real da aplicacao
 * (classpath:docs/*), indexado no startup pelo CorpusIndexer. Nao chama a Anthropic API.
 *
 * <p>"Relevante" e definido por documento-fonte (nao ha rotulos manuais por chunk).
 * Serve como guarda de regressao: se uma mudanca no embedder/chunker/store degradar o
 * retrieval, este teste falha.
 */
@SpringBootTest
class RetrievalEvaluationTest {

    private static final Logger log = LoggerFactory.getLogger(RetrievalEvaluationTest.class);
    private static final int K = 3;

    private record EvalCase(String question, String expectedSource) {
    }

    private static final List<EvalCase> CASES = List.of(
            new EvalCase("O que e auto-configuracao no Spring Boot?", "spring-boot-basics.md"),
            new EvalCase("O que e um starter no Spring Boot, como o spring-boot-starter-web?", "spring-boot-basics.md"),
            new EvalCase("Para que serve o endpoint /actuator/health?", "spring-boot-actuator.md"),
            new EvalCase("Como as metricas do Actuator se integram ao Prometheus?", "spring-boot-actuator.md"),
            new EvalCase("Como ativar um profile no Spring, como dev ou producao?", "spring-boot-profiles.md"),
            new EvalCase("O que e configuracao externalizada no Spring Boot?", "spring-boot-profiles.md"),
            new EvalCase("O que ganha uma interface ao estender JpaRepository?", "spring-data-jpa.md"),
            new EvalCase("O que sao query methods no Spring Data JPA?", "spring-data-jpa.md"));

    @Autowired
    private Embedder embedder;

    @Autowired
    private InMemoryVectorStore vectorStore;

    @Test
    void retrievalMeetsMinimumQualityBar() {
        assertThat(vectorStore.size()).isGreaterThan(0);

        double totalPrecision = 0.0;
        double totalReciprocalRank = 0.0;

        for (EvalCase evalCase : CASES) {
            double[] queryVector = embedder.embed(evalCase.question());
            List<ScoredChunk> retrieved = vectorStore.search(queryVector, K);

            double precision = RetrievalMetrics.precisionAtK(retrieved,
                    chunk -> chunk.source().equals(evalCase.expectedSource()));
            double reciprocalRank = RetrievalMetrics.reciprocalRank(retrieved,
                    chunk -> chunk.source().equals(evalCase.expectedSource()));

            log.info("pergunta='{}' esperado='{}' precision@{}={} reciprocalRank={}",
                    evalCase.question(), evalCase.expectedSource(), K, precision, reciprocalRank);

            totalPrecision += precision;
            totalReciprocalRank += reciprocalRank;
        }

        double meanPrecision = totalPrecision / CASES.size();
        double meanReciprocalRank = totalReciprocalRank / CASES.size();

        log.info("Retrieval eval: meanPrecision@{}={} MRR={} ({} perguntas)",
                K, meanPrecision, meanReciprocalRank, CASES.size());

        assertThat(meanPrecision).isGreaterThanOrEqualTo(0.3);
        assertThat(meanReciprocalRank).isGreaterThanOrEqualTo(0.5);
    }
}
