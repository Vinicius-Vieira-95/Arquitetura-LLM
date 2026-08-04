package com.example.ragdemo.rag;

import com.example.ragdemo.embedding.Embedder;
import com.example.ragdemo.llm.AnthropicClient;
import com.example.ragdemo.store.InMemoryVectorStore;
import com.example.ragdemo.store.ScoredChunk;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * O coracao do RAG. Fase ONLINE (a cada pergunta), em tres passos:
 *
 * <pre>
 *   1. RETRIEVE  -> vetoriza a pergunta e busca os top-k chunks mais similares
 *   2. AUGMENT   -> monta um prompt que injeta esses chunks como contexto
 *   3. GENERATE  -> pede ao modelo uma resposta ANCORADA nesse contexto
 * </pre>
 *
 * A ideia central do RAG: o modelo nao responde "de cabeca"; ele responde a partir
 * de trechos recuperados de uma base de conhecimento, o que reduz alucinacao e
 * permite citar fontes.
 */
@Service
public class RagService {

    private final Embedder embedder;
    private final InMemoryVectorStore vectorStore;
    private final AnthropicClient anthropicClient;
    private final MeterRegistry meterRegistry;
    private final int defaultTopK;

    public RagService(Embedder embedder,
                      InMemoryVectorStore vectorStore,
                      AnthropicClient anthropicClient,
                      MeterRegistry meterRegistry,
                      @Value("${rag.top-k:3}") int defaultTopK) {
        this.embedder = embedder;
        this.vectorStore = vectorStore;
        this.anthropicClient = anthropicClient;
        this.meterRegistry = meterRegistry;
        this.defaultTopK = defaultTopK;
    }

    public RagAnswer ask(String question, Integer topK) {
        Timer.Sample totalSample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            int k = (topK != null && topK > 0) ? topK : defaultTopK;

            // 1) RETRIEVE
            Timer.Sample retrieveSample = Timer.start(meterRegistry);
            double[] queryVector = embedder.embed(question);
            List<ScoredChunk> retrieved = vectorStore.search(queryVector, k);
            retrieveSample.stop(meterRegistry.timer("rag.retrieve"));

            // 2) AUGMENT
            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(question, retrieved);

            // 3) GENERATE
            Timer.Sample generateSample = Timer.start(meterRegistry);
            String answer;
            try {
                answer = anthropicClient.generate(systemPrompt, userPrompt);
            } finally {
                generateSample.stop(meterRegistry.timer("rag.generate"));
            }

            return new RagAnswer(question, answer, retrieved);
        } catch (RuntimeException e) {
            outcome = "error";
            meterRegistry.counter("rag.errors").increment();
            throw e;
        } finally {
            totalSample.stop(meterRegistry.timer("rag.ask", "outcome", outcome));
        }
    }

    private String buildSystemPrompt() {
        return """
                Voce e um assistente que responde APENAS com base no contexto fornecido.
                Regras:
                - Use somente as informacoes dos trechos de contexto numerados.
                - Se a resposta nao estiver no contexto, diga claramente que a informacao
                  nao foi encontrada na base de conhecimento. Nao invente.
                - Cite as fontes usadas referenciando o numero do trecho, por exemplo: [1], [2].
                - Responda em portugues, de forma objetiva.
                """;
    }

    private String buildUserPrompt(String question, List<ScoredChunk> retrieved) {
        StringBuilder context = new StringBuilder();
        int i = 1;
        for (ScoredChunk sc : retrieved) {
            context.append("[").append(i++).append("] (fonte: ").append(sc.chunk().source()).append(")\n");
            context.append(sc.chunk().text()).append("\n\n");
        }

        return """
                Contexto recuperado:
                %s
                Pergunta: %s
                """.formatted(context.toString().strip(), question);
    }
}
