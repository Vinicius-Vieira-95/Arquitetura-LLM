package com.example.ragfc.service;

import br.uece.llm.llm.LlmClient;
import br.uece.llm.model.ContentBlock;
import br.uece.llm.model.Message;
import br.uece.llm.model.ToolSpec;
import br.uece.llm.tool.ToolCallTrace;
import br.uece.llm.tool.ToolRegistry;
import com.example.ragdemo.embedding.Embedder;
import com.example.ragdemo.store.InMemoryVectorStore;
import com.example.ragdemo.store.ScoredChunk;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orquestrador de RAG + Function Calling. Combina as duas fases:
 *
 *  1. RETRIEVE inicial: recupera os top-k chunks mais relevantes para a pergunta
 *     (igual ao RagService de rag-spring-demo) e monta a primeira mensagem do
 *     usuario ja com esse contexto ("prompt aumentado").
 *  2. Ciclo de Function Calling (igual ao ChatService de function-calling-demo):
 *     o modelo recebe o historico + as specs de todas as ferramentas do
 *     ToolRegistry — incluindo {@code search_documents} (RagRetrievalTool) junto
 *     de calculate/get_weather/convert_currency — e decide se usa alguma.
 *
 * A diferenca central para "RAG puro" (RagService): la o contexto e fixo, recuperado
 * uma unica vez antes de chamar o modelo. Aqui o modelo pode chamar search_documents
 * de novo, com uma query mais especifica, se o contexto inicial nao bastar.
 */
@Service
public class RagFunctionCallingService {

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final Embedder embedder;
    private final InMemoryVectorStore vectorStore;
    private final MeterRegistry meterRegistry;
    private final int maxIterations;
    private final int topK;

    public RagFunctionCallingService(
            LlmClient llmClient,
            ToolRegistry toolRegistry,
            Embedder embedder,
            InMemoryVectorStore vectorStore,
            MeterRegistry meterRegistry,
            @Value("${llm.max-iterations:5}") int maxIterations,
            @Value("${rag.top-k:3}") int topK) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.embedder = embedder;
        this.vectorStore = vectorStore;
        this.meterRegistry = meterRegistry;
        this.maxIterations = maxIterations;
        this.topK = topK;
    }

    public RagFcResult chat(String userQuestion) {
        Timer.Sample totalSample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            // 1) RETRIEVE inicial
            Timer.Sample retrieveSample = Timer.start(meterRegistry);
            double[] questionVector = embedder.embed(userQuestion);
            List<ScoredChunk> initialChunks = vectorStore.search(questionVector, topK);
            retrieveSample.stop(meterRegistry.timer("ragfc.retrieve"));

            // 2) AUGMENT: monta a primeira mensagem ja com o contexto recuperado
            List<Message> history = new ArrayList<>();
            history.add(Message.user(buildAugmentedPrompt(userQuestion, initialChunks)));

            List<ToolSpec> tools = toolRegistry.specs();
            List<ToolCallTrace> trace = new ArrayList<>();

            // 3) Ciclo de Function Calling (identico ao de function-calling-demo)
            for (int iteration = 1; iteration <= maxIterations; iteration++) {
                Message assistant = llmClient.complete(history, tools);
                history.add(assistant);

                List<ContentBlock.ToolUse> toolUses = assistant.content().stream()
                        .filter(block -> block instanceof ContentBlock.ToolUse)
                        .map(block -> (ContentBlock.ToolUse) block)
                        .toList();

                if (toolUses.isEmpty()) {
                    meterRegistry.summary("ragfc.iterations").record(iteration);
                    return new RagFcResult(extractText(assistant), trace, iteration, initialChunks);
                }

                List<ContentBlock> results = new ArrayList<>();
                for (ContentBlock.ToolUse call : toolUses) {
                    Timer.Sample toolSample = Timer.start(meterRegistry);
                    ToolRegistry.Execution exec = toolRegistry.execute(call.name(), call.input());
                    toolSample.stop(meterRegistry.timer("ragfc.tool",
                            "tool", call.name(),
                            "outcome", exec.isError() ? "error" : "success"));
                    results.add(new ContentBlock.ToolResult(call.id(), exec.output(), exec.isError()));
                    trace.add(new ToolCallTrace(call.name(), call.input(), exec.output(), exec.isError()));
                }
                history.add(Message.toolResults(results));
            }

            outcome = "iteration_limit";
            meterRegistry.summary("ragfc.iterations").record(maxIterations);
            return new RagFcResult(
                    "Limite de iteracoes (" + maxIterations + ") atingido sem resposta final.",
                    trace,
                    maxIterations,
                    initialChunks);
        } catch (RuntimeException e) {
            outcome = "error";
            meterRegistry.counter("ragfc.errors").increment();
            throw e;
        } finally {
            totalSample.stop(meterRegistry.timer("ragfc.chat", "outcome", outcome));
        }
    }

    private String buildAugmentedPrompt(String question, List<ScoredChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        if (!chunks.isEmpty()) {
            sb.append("CONTEXTO RECUPERADO (base de conhecimento):\n");
            int i = 1;
            for (ScoredChunk chunk : chunks) {
                sb.append("[").append(i++).append("] (fonte: ").append(chunk.chunk().source()).append(")\n")
                        .append(chunk.chunk().text())
                        .append("\n\n");
            }
            sb.append("Se este contexto nao for suficiente, use a ferramenta search_documents ")
                    .append("com uma consulta mais especifica antes de responder.\n---\n\n");
        }
        sb.append("PERGUNTA: ").append(question);
        return sb.toString();
    }

    private String extractText(Message message) {
        return message.content().stream()
                .filter(block -> block instanceof ContentBlock.Text)
                .map(block -> ((ContentBlock.Text) block).text())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }
}
