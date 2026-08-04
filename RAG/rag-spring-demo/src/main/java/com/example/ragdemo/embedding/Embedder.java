package com.example.ragdemo.embedding;

import java.util.List;

/**
 * Abstrai a etapa de vetorizacao do RAG.
 *
 * <p>Esta e a fronteira que isola "como transformamos texto em vetor". A demo usa
 * uma implementacao TF-IDF (lexica) feita do zero, mas trocar por um embedder
 * semantico de verdade (Voyage AI, OpenAI, um modelo local via ONNX/DJL...) e so
 * criar outra classe que implemente esta interface e registra-la como bean.
 *
 * <p>Observacao: TF-IDF exige "ajustar" o vocabulario/IDF ao corpus antes de
 * vetorizar (metodo {@link #fit(List)}). Um embedder neural pre-treinado NAO
 * precisaria disso — bastaria implementar {@link #embed(String)} e deixar
 * {@link #fit(List)} vazio.
 */
public interface Embedder {

    /** Ajusta o embedder ao corpus (no TF-IDF: monta vocabulario e calcula IDF). */
    void fit(List<String> documents);

    /** Converte um texto em um vetor (idealmente ja normalizado em L2). */
    double[] embed(String text);

    /** Dimensionalidade dos vetores gerados. */
    int dimension();
}
