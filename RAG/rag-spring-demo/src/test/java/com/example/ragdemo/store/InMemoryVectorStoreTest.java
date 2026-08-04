package com.example.ragdemo.store;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryVectorStoreTest {

    @Test
    void searchOnEmptyStoreReturnsEmptyList() {
        InMemoryVectorStore store = new InMemoryVectorStore();

        List<ScoredChunk> result = store.search(new double[]{1.0, 0.0}, 3);

        assertThat(result).isEmpty();
    }

    @Test
    void searchReturnsResultsOrderedByDescendingScore() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.add(new Chunk("a", "docA", "texto a"), new double[]{1.0, 0.0});
        store.add(new Chunk("b", "docB", "texto b"), new double[]{0.0, 1.0});
        store.add(new Chunk("c", "docC", "texto c"), new double[]{0.7, 0.7});

        List<ScoredChunk> result = store.search(new double[]{1.0, 0.0}, 3);

        assertThat(result).extracting(sc -> sc.chunk().id())
                .containsExactly("a", "c", "b");
        assertThat(result.get(0).score()).isGreaterThan(result.get(1).score());
        assertThat(result.get(1).score()).isGreaterThan(result.get(2).score());
    }

    @Test
    void searchWithKGreaterThanSizeReturnsAllEntries() {
        InMemoryVectorStore store = new InMemoryVectorStore();
        store.add(new Chunk("a", "docA", "texto a"), new double[]{1.0, 0.0});
        store.add(new Chunk("b", "docB", "texto b"), new double[]{0.0, 1.0});

        List<ScoredChunk> result = store.search(new double[]{1.0, 0.0}, 10);

        assertThat(result).hasSize(2);
        assertThat(store.size()).isEqualTo(2);
    }
}
