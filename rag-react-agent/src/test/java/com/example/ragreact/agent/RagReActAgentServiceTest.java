package com.example.ragreact.agent;

import com.example.ragdemo.embedding.TfidfEmbedder;
import com.example.ragdemo.store.Chunk;
import com.example.ragdemo.store.InMemoryVectorStore;
import com.example.ragreact.support.FakeLlmClient;
import com.example.ragreact.tool.CalculatorTool;
import com.example.ragreact.tool.RagSearchTool;
import com.example.ragreact.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o ciclo completo de RAG + ReAct com o FakeLlmClient (sem chamar a API real):
 * RETRIEVE inicial -> Question aumentada -> ciclo Thought/Action/Observation, com
 * search_documents e calculate disponiveis como Actions.
 */
class RagReActAgentServiceTest {

    private TfidfEmbedder embedder;
    private InMemoryVectorStore vectorStore;
    private RagReActAgentService service;

    @BeforeEach
    void setUp() {
        embedder = new TfidfEmbedder();
        vectorStore = new InMemoryVectorStore();

        List<String> corpus = List.of(
                "O endpoint de health do Actuator informa se a aplicacao esta saudavel.",
                "Documentos aceitos para isencao incluem carteira de identidade e passaporte."
        );
        embedder.fit(corpus);
        vectorStore.add(new Chunk("doc1#0", "doc1.md", corpus.get(0)), embedder.embed(corpus.get(0)));
        vectorStore.add(new Chunk("doc2#0", "doc2.md", corpus.get(1)), embedder.embed(corpus.get(1)));

        ToolRegistry registry = new ToolRegistry(List.of(
                new RagSearchTool(embedder, vectorStore),
                new CalculatorTool()));

        service = new RagReActAgentService(
                new FakeLlmClient(), registry, embedder, vectorStore, new SimpleMeterRegistry(), 5, 3);
    }

    @Test
    void respondeComContextoInicialSemChamarAction() {
        RagReActResult result = service.run("Para que serve o endpoint de health?");

        assertTrue(result.steps().isEmpty());
        assertEquals(1, result.iterations());
        assertFalse(result.initialRetrieval().isEmpty());
        assertTrue(result.answer().toLowerCase().contains("contexto"));
    }

    @Test
    void chamaSearchDocumentsQuandoModeloPedeParaRefinar() {
        RagReActResult result = service.run("REFINAR: documentos para isencao");

        assertEquals(1, result.steps().size());
        assertEquals("search_documents", result.steps().get(0).action());
        assertFalse(result.steps().get(0).isError());
        assertTrue(result.answer().toLowerCase().contains("passaporte"));
        assertEquals(2, result.iterations());
    }

    @Test
    void chamaCalculatorToolLocal() {
        RagReActResult result = service.run("Quanto e (12 + 7) * 3?");

        assertEquals(1, result.steps().size());
        assertEquals("calculate", result.steps().get(0).action());
        assertTrue(result.steps().get(0).observation().contains("57"));
    }
}
