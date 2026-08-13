package com.example.ragdemo.ingest;

import com.example.ragdemo.store.Chunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkerTest {

    @Test
    void singleShortParagraphProducesOneChunk() {
        Chunker chunker = new Chunker(600);

        List<Chunk> chunks = chunker.chunk("doc.md", "Um paragrafo curto.");

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).id()).isEqualTo("doc.md#0");
        assertThat(chunks.get(0).source()).isEqualTo("doc.md");
        assertThat(chunks.get(0).text()).isEqualTo("Um paragrafo curto.");
    }

    @Test
    void paragraphsExceedingMaxCharsAreSplitIntoMultipleChunks() {
        Chunker chunker = new Chunker(20);
        String text = "Primeiro paragrafo aqui.\n\nSegundo paragrafo aqui.\n\nTerceiro paragrafo aqui.";

        List<Chunk> chunks = chunker.chunk("doc.md", text);

        assertThat(chunks).hasSizeGreaterThan(1);
        for (int i = 0; i < chunks.size(); i++) {
            assertThat(chunks.get(i).id()).isEqualTo("doc.md#" + i);
        }
    }

    @Test
    void paragraphsWithinLimitAreGroupedInSameChunk() {
        Chunker chunker = new Chunker(600);
        String text = "Paragrafo um.\n\nParagrafo dois.\n\nParagrafo tres.";

        List<Chunk> chunks = chunker.chunk("doc.md", text);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).contains("Paragrafo um.", "Paragrafo dois.", "Paragrafo tres.");
    }

    @Test
    void blankParagraphsAreIgnored() {
        Chunker chunker = new Chunker(600);
        String text = "Paragrafo um.\n\n   \n\nParagrafo dois.";

        List<Chunk> chunks = chunker.chunk("doc.md", text);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).isEqualTo("Paragrafo um.\n\nParagrafo dois.");
    }
}
