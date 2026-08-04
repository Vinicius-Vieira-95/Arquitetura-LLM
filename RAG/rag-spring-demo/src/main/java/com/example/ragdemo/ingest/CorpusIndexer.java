package com.example.ragdemo.ingest;

import com.example.ragdemo.embedding.Embedder;
import com.example.ragdemo.store.Chunk;
import com.example.ragdemo.store.InMemoryVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Fase de INDEXACAO (offline) do RAG, executada uma vez no startup:
 *
 * <pre>
 *   documentos  ->  chunks  ->  fit do embedder  ->  vetores  ->  vector store
 * </pre>
 *
 * Carrega todos os arquivos de {@code classpath:docs/*}, divide em chunks, ajusta o
 * embedder ao corpus e armazena (chunk + vetor) no vector store.
 */
@Component
public class CorpusIndexer {

    private static final Logger log = LoggerFactory.getLogger(CorpusIndexer.class);

    private final Chunker chunker;
    private final Embedder embedder;
    private final InMemoryVectorStore vectorStore;

    public CorpusIndexer(Chunker chunker, Embedder embedder, InMemoryVectorStore vectorStore) {
        this.chunker = chunker;
        this.embedder = embedder;
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void index() throws Exception {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] docs = resolver.getResources("classpath:docs/*");

        // 1) Carrega documentos e quebra em chunks.
        List<Chunk> chunks = new ArrayList<>();
        for (Resource doc : docs) {
            String source = doc.getFilename();
            String content;
            try (var in = doc.getInputStream()) {
                content = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            }
            chunks.addAll(chunker.chunk(source, content));
        }

        if (chunks.isEmpty()) {
            log.warn("Nenhum documento encontrado em classpath:docs/* — o RAG nao tera contexto.");
            return;
        }

        // 2) Ajusta o embedder ao corpus (necessario para TF-IDF; inocuo para embedders neurais).
        List<String> corpus = chunks.stream().map(Chunk::text).toList();
        embedder.fit(corpus);

        // 3) Vetoriza cada chunk e armazena no vector store.
        for (Chunk chunk : chunks) {
            vectorStore.add(chunk, embedder.embed(chunk.text()));
        }

        log.info("Indexacao concluida: {} documentos, {} chunks, dimensao do vetor = {}.",
                docs.length, vectorStore.size(), embedder.dimension());
    }
}
