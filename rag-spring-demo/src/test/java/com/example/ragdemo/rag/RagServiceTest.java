package com.example.ragdemo.rag;

import com.example.ragdemo.embedding.Embedder;
import com.example.ragdemo.llm.AnthropicClient;
import com.example.ragdemo.store.Chunk;
import com.example.ragdemo.store.InMemoryVectorStore;
import com.example.ragdemo.store.ScoredChunk;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagServiceTest {

    private static final int DEFAULT_TOP_K = 3;

    private Embedder embedder;
    private InMemoryVectorStore vectorStore;
    private AnthropicClient anthropicClient;
    private RagService ragService;

    @BeforeEach
    void setUp() {
        embedder = mock(Embedder.class);
        vectorStore = mock(InMemoryVectorStore.class);
        anthropicClient = mock(AnthropicClient.class);
        ragService = new RagService(embedder, vectorStore, anthropicClient,
                new SimpleMeterRegistry(), DEFAULT_TOP_K);
    }

    @Test
    void askUsesDefaultTopKWhenNoneProvided() {
        double[] vector = {1.0, 0.0};
        List<ScoredChunk> retrieved = List.of(
                new ScoredChunk(new Chunk("doc#0", "doc.md", "conteudo"), 0.9));
        when(embedder.embed("pergunta")).thenReturn(vector);
        when(vectorStore.search(eq(vector), anyInt())).thenReturn(retrieved);
        when(anthropicClient.generate(any(), any())).thenReturn("resposta gerada");

        RagAnswer answer = ragService.ask("pergunta", null);

        verify(vectorStore).search(vector, DEFAULT_TOP_K);
        assertThat(answer.question()).isEqualTo("pergunta");
        assertThat(answer.answer()).isEqualTo("resposta gerada");
        assertThat(answer.retrieved()).isEqualTo(retrieved);
    }

    @Test
    void askUsesExplicitTopKWhenProvided() {
        double[] vector = {0.0, 1.0};
        when(embedder.embed("pergunta")).thenReturn(vector);
        when(vectorStore.search(eq(vector), anyInt())).thenReturn(List.of());
        when(anthropicClient.generate(any(), any())).thenReturn("resposta");

        ragService.ask("pergunta", 7);

        verify(vectorStore).search(vector, 7);
    }

    @Test
    void askInjectsRetrievedChunksIntoUserPrompt() {
        double[] vector = {1.0};
        List<ScoredChunk> retrieved = List.of(
                new ScoredChunk(new Chunk("doc.md#0", "doc.md", "trecho relevante"), 0.8));
        when(embedder.embed(any())).thenReturn(vector);
        when(vectorStore.search(any(), anyInt())).thenReturn(retrieved);
        when(anthropicClient.generate(any(), any())).thenReturn("resposta");

        ragService.ask("qual e o trecho?", null);

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropicClient).generate(any(), userPromptCaptor.capture());
        assertThat(userPromptCaptor.getValue())
                .contains("trecho relevante")
                .contains("doc.md")
                .contains("qual e o trecho?");
    }
}
