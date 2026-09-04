package com.example.ragfc.tool;

import br.uece.llm.model.ToolSpec;
import br.uece.llm.tool.Tool;
import com.example.ragdemo.embedding.Embedder;
import com.example.ragdemo.store.InMemoryVectorStore;
import com.example.ragdemo.store.ScoredChunk;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Expoe a busca RAG (embed + similaridade de cosseno no InMemoryVectorStore) como
 * ferramenta de Function Calling. E o que diferencia esta integracao de "RAG puro":
 * o modelo, e nao a aplicacao, decide quando precisa buscar mais contexto — pode
 * chamar {@code search_documents} de novo com uma query mais especifica, em vez de
 * ficar preso aos chunks recuperados uma unica vez no inicio do ciclo.
 */
@Component
public class RagRetrievalTool implements Tool {

    private static final int DEFAULT_LIMIT = 3;

    private final Embedder embedder;
    private final InMemoryVectorStore vectorStore;

    public RagRetrievalTool(Embedder embedder, InMemoryVectorStore vectorStore) {
        this.embedder = embedder;
        this.vectorStore = vectorStore;
    }

    @Override
    public String name() {
        return "search_documents";
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec(
                name(),
                "Busca trechos na base de conhecimento indexada (RAG). Use para recuperar "
                        + "contexto adicional quando o contexto inicial fornecido nao for suficiente "
                        + "para responder com precisao, ou para refinar a busca com uma query mais especifica.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of(
                                        "type", "string",
                                        "description", "Texto da busca, ex.: 'documentos aceitos para isencao'"
                                ),
                                "limit", Map.of(
                                        "type", "integer",
                                        "description", "Numero maximo de resultados (padrao: 3)"
                                )
                        ),
                        "required", List.of("query")
                )
        );
    }

    @Override
    public String execute(Map<String, Object> input) {
        String query = String.valueOf(input.getOrDefault("query", "")).trim();
        if (query.isEmpty()) {
            throw new IllegalArgumentException("Parametro 'query' e' obrigatorio.");
        }
        int limit = input.get("limit") instanceof Number n ? n.intValue() : DEFAULT_LIMIT;

        double[] queryVector = embedder.embed(query);
        List<ScoredChunk> results = vectorStore.search(queryVector, limit);
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
