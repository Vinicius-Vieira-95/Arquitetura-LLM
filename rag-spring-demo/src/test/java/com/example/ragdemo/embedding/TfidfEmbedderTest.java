package com.example.ragdemo.embedding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TfidfEmbedderTest {

    @Test
    void embedBeforeFitThrows() {
        TfidfEmbedder embedder = new TfidfEmbedder();

        assertThatThrownBy(() -> embedder.embed("qualquer texto"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void embedReturnsL2NormalizedVector() {
        TfidfEmbedder embedder = new TfidfEmbedder();
        embedder.fit(List.of("spring boot facilita configuracao", "spring data jpa acessa dados"));

        double[] vector = embedder.embed("spring boot configuracao");

        double norm = Math.sqrt(sumSquares(vector));
        assertThat(norm).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void identicalTextsHaveCosineSimilarityCloseToOne() {
        TfidfEmbedder embedder = new TfidfEmbedder();
        embedder.fit(List.of("spring boot facilita configuracao", "spring data jpa acessa dados"));

        double[] a = embedder.embed("spring boot facilita configuracao");
        double[] b = embedder.embed("spring boot facilita configuracao");

        assertThat(cosine(a, b)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void unrelatedTextsHaveLowerSimilarityThanIdenticalTexts() {
        TfidfEmbedder embedder = new TfidfEmbedder();
        embedder.fit(List.of(
                "spring boot facilita configuracao de aplicacoes",
                "spring data jpa acessa bancos de dados relacionais"));

        double[] question = embedder.embed("spring boot facilita configuracao");
        double[] same = embedder.embed("spring boot facilita configuracao");
        double[] different = embedder.embed("spring data jpa acessa bancos");

        assertThat(cosine(question, different)).isLessThan(cosine(question, same));
    }

    @Test
    void dimensionMatchesVocabularySize() {
        TfidfEmbedder embedder = new TfidfEmbedder();
        embedder.fit(List.of("um dois tres", "quatro cinco"));

        assertThat(embedder.dimension()).isEqualTo(5);
        assertThat(embedder.embed("um dois").length).isEqualTo(embedder.dimension());
    }

    private static double sumSquares(double[] v) {
        double sum = 0.0;
        for (double x : v) {
            sum += x * x;
        }
        return sum;
    }

    private static double cosine(double[] a, double[] b) {
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }
}
