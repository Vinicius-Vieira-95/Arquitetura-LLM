package com.example.ragreact.tool;

import com.example.ragdemo.embedding.TfidfEmbedder;
import com.example.ragdemo.store.Chunk;
import com.example.ragdemo.store.InMemoryVectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagSearchToolTest {

    private RagSearchTool tool;

    @BeforeEach
    void setUp() {
        TfidfEmbedder embedder = new TfidfEmbedder();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();

        List<String> corpus = List.of(
                "O endpoint de health do Actuator informa se a aplicacao esta saudavel.",
                "Documentos aceitos para isencao incluem carteira de identidade e passaporte."
        );
        embedder.fit(corpus);
        vectorStore.add(new Chunk("doc1#0", "doc1.md", corpus.get(0)), embedder.embed(corpus.get(0)));
        vectorStore.add(new Chunk("doc2#0", "doc2.md", corpus.get(1)), embedder.embed(corpus.get(1)));

        tool = new RagSearchTool(embedder, vectorStore);
    }

    @Test
    void retornaChunkMaisRelevante() {
        String output = tool.execute("passaporte identidade");
        assertTrue(output.contains("passaporte"));
    }

    @Test
    void actionInputVazioLancaErro() {
        assertThrows(IllegalArgumentException.class, () -> tool.execute(""));
    }
}
