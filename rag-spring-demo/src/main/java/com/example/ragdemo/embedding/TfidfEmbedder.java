package com.example.ragdemo.embedding;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Embedder TF-IDF implementado do zero (sem libs de ML).
 *
 * <p>Serve para deixar VISIVEL o principio de recuperacao do RAG: texto vira vetor,
 * vetores sao comparados por cosseno. NAO captura semantica (sinonimos, parafrases);
 * e essencialmente busca lexica ponderada. Para producao, troque por um embedder
 * semantico implementando a interface {@link Embedder}.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>fit: tokeniza o corpus, monta o vocabulario e calcula o IDF de cada termo;</li>
 *   <li>embed: monta o vetor TF do texto, pondera por IDF e normaliza em L2
 *       (assim o produto escalar entre dois vetores ja e o cosseno).</li>
 * </ol>
 */
@Component
public class TfidfEmbedder implements Embedder {

    private Map<String, Integer> vocabulary = new HashMap<>();
    private double[] idf = new double[0];
    private boolean fitted = false;

    @Override
    public void fit(List<String> documents) {
        vocabulary = new HashMap<>();

        // Frequencia de documentos (em quantos documentos cada termo aparece).
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (String doc : documents) {
            Set<String> termsInDoc = new HashSet<>(tokenize(doc));
            for (String term : termsInDoc) {
                vocabulary.computeIfAbsent(term, t -> vocabulary.size());
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }

        // IDF suavizado: idf(t) = ln((N + 1) / (df(t) + 1)) + 1
        int n = documents.size();
        idf = new double[vocabulary.size()];
        for (Map.Entry<String, Integer> entry : vocabulary.entrySet()) {
            int df = documentFrequency.getOrDefault(entry.getKey(), 0);
            idf[entry.getValue()] = Math.log((double) (n + 1) / (df + 1)) + 1.0;
        }

        fitted = true;
    }

    @Override
    public double[] embed(String text) {
        if (!fitted) {
            throw new IllegalStateException("Embedder nao foi ajustado: chame fit(corpus) antes de embed().");
        }

        // Term frequency, ignorando termos fora do vocabulario (ex.: palavras novas na pergunta).
        double[] vector = new double[vocabulary.size()];
        for (String term : tokenize(text)) {
            Integer index = vocabulary.get(term);
            if (index != null) {
                vector[index] += 1.0;
            }
        }

        // Pondera por IDF.
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= idf[i];
        }

        return l2Normalize(vector);
    }

    @Override
    public int dimension() {
        return vocabulary.size();
    }

    /** Tokenizacao simples: minusculas e quebra em sequencias de letras/digitos (preserva acentos). */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (char c : text.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                current.append(c);
            } else if (current.length() > 0) {
                if (current.length() > 1) {
                    tokens.add(current.toString());
                }
                current.setLength(0);
            }
        }
        if (current.length() > 1) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    /** Normalizacao L2: deixa o vetor com norma 1, para que dot(a, b) == cosseno(a, b). */
    private double[] l2Normalize(double[] vector) {
        double sumSquares = 0.0;
        for (double v : vector) {
            sumSquares += v * v;
        }
        double norm = Math.sqrt(sumSquares);
        if (norm == 0.0) {
            return vector; // vetor nulo (nenhum termo conhecido): retorna como esta.
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] /= norm;
        }
        return vector;
    }
}
