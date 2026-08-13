package com.example.ragdemo.store;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Vector store em memoria (o "banco de vetores" mais simples possivel).
 *
 * <p>Guarda cada chunk junto do seu vetor e faz busca top-k por similaridade de
 * cosseno via forca bruta. Para um corpus pequeno isso e perfeitamente adequado e
 * deixa o principio transparente. Em escala, este componente seria substituido por
 * um indice vetorial de verdade (pgvector, Qdrant, Elasticsearch kNN, FAISS...),
 * que usa indices aproximados (HNSW/IVF) em vez de comparar com todos os vetores.
 */
@Component
public class InMemoryVectorStore {

    private record Entry(Chunk chunk, double[] vector) {
    }

    private final List<Entry> entries = new ArrayList<>();

    public void add(Chunk chunk, double[] vector) {
        entries.add(new Entry(chunk, vector));
    }

    public int size() {
        return entries.size();
    }

    /** Retorna os {@code k} chunks mais similares a {@code queryVector}, do maior para o menor score. */
    public List<ScoredChunk> search(double[] queryVector, int k) {
        return entries.stream()
                .map(e -> new ScoredChunk(e.chunk(), cosine(queryVector, e.vector())))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(k)
                .toList();
    }

    /**
     * Similaridade de cosseno. Como os vetores ja chegam normalizados em L2 do embedder,
     * isto e equivalente ao produto escalar — mas calculamos a forma completa para que o
     * store funcione mesmo com vetores nao normalizados.
     */
    private double cosine(double[] a, double[] b) {
        int len = Math.min(a.length, b.length);
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
