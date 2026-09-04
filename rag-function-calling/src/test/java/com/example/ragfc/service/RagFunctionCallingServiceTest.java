package com.example.ragfc.service;

import br.uece.llm.tool.CalculatorTool;
import br.uece.llm.tool.ToolRegistry;
import com.example.ragdemo.embedding.TfidfEmbedder;
import com.example.ragdemo.store.Chunk;
import com.example.ragdemo.store.InMemoryVectorStore;
import com.example.ragfc.support.FakeLlmClient;
import com.example.ragfc.tool.RagRetrievalTool;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o ciclo completo de RAG + Function Calling com o FakeLlmClient (sem chamar a
 * API real): RETRIEVE inicial -> prompt aumentado -> ciclo de Function Calling, com
 * search_documents e calculate disponiveis como ferramentas.
 */
class RagFunctionCallingServiceTest {

    private TfidfEmbedder embedder;
    private InMemoryVectorStore vectorStore;
    private RagFunctionCallingService service;

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
                new RagRetrievalTool(embedder, vectorStore),
                new CalculatorTool()));

        service = new RagFunctionCallingService(
                new FakeLlmClient(), registry, embedder, vectorStore, new SimpleMeterRegistry(), 5, 3);
    }

    @Test
    void respondeComContextoInicialSemChamarFerramenta() {
        RagFcResult result = service.chat("Para que serve o endpoint de health?");

        assertTrue(result.toolCalls().isEmpty());
        assertEquals(1, result.iterations());
        assertFalse(result.initialRetrieval().isEmpty());
        assertTrue(result.answer().toLowerCase().contains("contexto"));
    }

    @Test
    void chamaSearchDocumentsQuandoModeloPedeParaRefinar() {
        RagFcResult result = service.chat("REFINAR: documentos para isencao");

        assertEquals(1, result.toolCalls().size());
        assertEquals("search_documents", result.toolCalls().get(0).tool());
        assertFalse(result.toolCalls().get(0).isError());
        assertTrue(result.answer().toLowerCase().contains("passaporte"));
        assertEquals(2, result.iterations());
    }

    @Test
    void chamaCalculatorToolDoCommon() {
        RagFcResult result = service.chat("Quanto e (12 + 7) * 3?");

        assertEquals(1, result.toolCalls().size());
        assertEquals("calculate", result.toolCalls().get(0).tool());
        assertTrue(result.toolCalls().get(0).output().contains("57"));
    }
}
