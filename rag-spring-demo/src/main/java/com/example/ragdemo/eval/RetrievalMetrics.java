package com.example.ragdemo.eval;

import com.example.ragdemo.store.Chunk;
import com.example.ragdemo.store.ScoredChunk;

import java.util.List;
import java.util.function.Predicate;

/**
 * Metricas de qualidade de retrieval, calculadas sobre uma lista de {@link ScoredChunk}
 * ja ordenada por relevancia (a ordem retornada por {@code InMemoryVectorStore.search}).
 *
 * <p>"Relevante" e definido pelo chamador via {@link Predicate}, o que permite tanto
 * granularidade por documento (fonte) quanto, futuramente, por chunk especifico.
 */
public final class RetrievalMetrics {

    private RetrievalMetrics() {
    }

    /** Fracao dos {@code k} chunks recuperados que sao relevantes. */
    public static double precisionAtK(List<ScoredChunk> retrieved, Predicate<Chunk> isRelevant) {
        if (retrieved.isEmpty()) {
            return 0.0;
        }
        long relevantCount = retrieved.stream()
                .map(ScoredChunk::chunk)
                .filter(isRelevant)
                .count();
        return (double) relevantCount / retrieved.size();
    }

    /** 1 / posicao (1-indexada) do primeiro chunk relevante; 0 se nenhum for relevante. */
    public static double reciprocalRank(List<ScoredChunk> retrieved, Predicate<Chunk> isRelevant) {
        for (int i = 0; i < retrieved.size(); i++) {
            if (isRelevant.test(retrieved.get(i).chunk())) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }
}
