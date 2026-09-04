package com.example.ragreact.tool;

import com.example.ragdemo.embedding.Embedder;
import com.example.ragdemo.store.InMemoryVectorStore;
import com.example.ragdemo.store.ScoredChunk;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Expoe a busca RAG (embed + similaridade de cosseno no InMemoryVectorStore) como
 * Action do ciclo ReAct. Igual ao RagRetrievalTool de rag-function-calling em
 * proposito, mas com o contrato ReAct: Action Input e saida sao texto livre, nao
 * JSON estruturado. O modelo escreve o texto da busca diretamente como Action Input.
 */
@Component
public class RagSearchTool implements Tool {

    private static final int DEFAULT_LIMIT = 3;

    private final Embedder embedder;
    private final InMemoryVectorStore vectorStore;

    public RagSearchTool(Embedder embedder, InMemoryVectorStore vectorStore) {
        this.embedder = embedder;
        this.vectorStore = vectorStore;
    }

    @Override
    public String name() {
        return "search_documents";
    }

    @Override
    public String description() {
        return "Busca trechos na base de conhecimento indexada (RAG). Use quando o contexto inicial "
                + "fornecido na pergunta nao for suficiente para responder com precisao. "
                + "Action Input: o texto da busca, ex.: 'documentos aceitos para isencao'.";
    }

    @Override
    public String execute(String input) {
        String query = input == null ? "" : input.trim();
        if (query.isEmpty()) {
            throw new IllegalArgumentException("Informe o texto da busca.");
        }

        double[] queryVector = embedder.embed(query);
        List<ScoredChunk> results = vectorStore.search(queryVector, DEFAULT_LIMIT);
        return format(results);
    }

    private String format(List<ScoredChunk> chunks) {
        if (chunks.isEmpty()) {
            return "Nenhum documento encontrado para a consulta.";
        }
        StringBuilder sb = new StringBuilder("Documentos encontrados:\n");
        int i = 1;
        for (ScoredChunk chunk : chunks) {
            sb.append("\n[").append(i++).append("] (score: ")
                    .append("%.2f".formatted(chunk.score()))
                    .append(", fonte: ").append(chunk.chunk().source()).append(")\n")
                    .append(chunk.chunk().text())
                    .append("\n");
        }
        return sb.toString();
    }
}
