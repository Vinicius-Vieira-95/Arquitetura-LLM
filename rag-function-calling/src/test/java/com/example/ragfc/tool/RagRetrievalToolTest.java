package com.example.ragfc.tool;

import com.example.ragdemo.embedding.TfidfEmbedder;
import com.example.ragdemo.store.Chunk;
import com.example.ragdemo.store.InMemoryVectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagRetrievalToolTest {

    private RagRetrievalTool tool;

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

        tool = new RagRetrievalTool(embedder, vectorStore);
    }

    @Test
    void nomeEIgualAoUsadoNoContrato() {
        assertTrue(tool.name().equals("search_documents"));
    }

    @Test
    void retornaChunkMaisRelevantePrimeiro() {
        String output = tool.execute(Map.of("query", "passaporte identidade"));
        assertTrue(output.contains("passaporte"));
    }

    @Test
    void queryVaziaLancaErro() {
        assertThrows(IllegalArgumentException.class, () -> tool.execute(Map.of("query", "")));
    }
}
