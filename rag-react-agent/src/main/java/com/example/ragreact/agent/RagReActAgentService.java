package com.example.ragreact.agent;

import com.example.ragdemo.embedding.Embedder;
import com.example.ragdemo.store.InMemoryVectorStore;
import com.example.ragdemo.store.ScoredChunk;
import com.example.ragreact.llm.LlmClient;
import com.example.ragreact.tool.ToolRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orquestrador de RAG + ReAct. Combina as duas fases:
 *
 *  1. RETRIEVE inicial: recupera os top-k chunks mais relevantes para a pergunta
 *     (igual ao RagService de rag-spring-demo) e injeta esse contexto diretamente
 *     no campo "Question" do prompt ReAct.
 *  2. Ciclo ReAct (igual ao ReActAgentService de react-agent): o modelo alterna
 *     Thought/Action/Action Input/Observation ate produzir a Final Answer. A Action
 *     {@code search_documents} (RagSearchTool) fica disponivel ao lado de
 *     calculate/get_weather/convert_currency, para o modelo refinar a busca se o
 *     contexto inicial nao bastar.
 *
 * A diferenca central para "RAG puro" (RagService): la o contexto e fixo, recuperado
 * uma unica vez antes de chamar o modelo. Aqui o agente pode chamar search_documents
 * de novo, com uma query mais especifica, como mais uma Action do ciclo ReAct.
 */
@Service
public class RagReActAgentService {

    private static final String STOP_SEQUENCE = "\nObservation:";
    private static final Pattern ACTION_PATTERN = Pattern.compile("Action:\\s*(.+)");
    private static final Pattern ACTION_INPUT_PATTERN = Pattern.compile("Action Input:\\s*(.+)", Pattern.DOTALL);
    private static final String FINAL_ANSWER_MARKER = "Final Answer:";

    private static final String PROMPT_TEMPLATE = """
            Voce e' um agente que resolve tarefas usando o padrao ReAct (Reasoning + Acting),
            com acesso a uma base de conhecimento. Responda a pergunta do usuario da melhor forma
            possivel. Voce tem acesso as seguintes ferramentas:

            %s

            Use SEMPRE o seguinte formato:

            Question: a pergunta de entrada que voce deve responder
            Thought: voce deve sempre pensar sobre o que fazer a seguir
            Action: a acao a tomar, deve ser uma das seguintes: [%s]
            Action Input: a entrada para a acao
            Observation: o resultado da acao
            ... (esse padrao Thought/Action/Action Input/Observation pode se repetir quantas vezes forem necessarias)
            Thought: agora eu sei a resposta final
            Final Answer: a resposta final para a pergunta original

            IMPORTANTE:
            - A Question ja vem com um CONTEXTO RECUPERADO inicial da base de conhecimento; use-o.
            - Se esse contexto nao for suficiente, use search_documents com uma consulta mais
              especifica antes de responder.
            - Voce esta continuando uma transcricao ja iniciada. Responda APENAS com o texto que \
            continua a transcricao a partir do ponto em que ela para (que termina em "Thought:") — nao \
            repita a palavra "Thought", nao repita nada do que ja foi escrito, e pare antes de escrever \
            "Observation:" (o resultado da acao sera fornecido a voce na proxima rodada).

            Comece!

            Question: %s
            Thought:%s""";

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final Embedder embedder;
    private final InMemoryVectorStore vectorStore;
    private final MeterRegistry meterRegistry;
    private final int maxIterations;
    private final int topK;

    public RagReActAgentService(
            LlmClient llmClient,
            ToolRegistry toolRegistry,
            Embedder embedder,
            InMemoryVectorStore vectorStore,
            MeterRegistry meterRegistry,
            @Value("${react.max-iterations:5}") int maxIterations,
            @Value("${rag.top-k:3}") int topK) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.embedder = embedder;
        this.vectorStore = vectorStore;
        this.meterRegistry = meterRegistry;
        this.maxIterations = maxIterations;
        this.topK = topK;
    }

    public RagReActResult run(String question) {
        Timer.Sample totalSample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            // 1) RETRIEVE inicial
            Timer.Sample retrieveSample = Timer.start(meterRegistry);
            double[] questionVector = embedder.embed(question);
            List<ScoredChunk> initialChunks = vectorStore.search(questionVector, topK);
            retrieveSample.stop(meterRegistry.timer("ragreact.retrieve"));

            // 2) AUGMENT: injeta o contexto recuperado na propria Question
            String augmentedQuestion = buildAugmentedQuestion(question, initialChunks);

            StringBuilder scratchpad = new StringBuilder();
            List<AgentStep> steps = new ArrayList<>();

            // 3) Ciclo ReAct (identico ao de react-agent)
            for (int iteration = 1; iteration <= maxIterations; iteration++) {
                String prompt = buildPrompt(augmentedQuestion, scratchpad.toString());
                String completion = llmClient.complete(prompt, List.of(STOP_SEQUENCE)).stripTrailing();

                int finalAnswerIdx = completion.indexOf(FINAL_ANSWER_MARKER);
                if (finalAnswerIdx >= 0) {
                    String answer = completion.substring(finalAnswerIdx + FINAL_ANSWER_MARKER.length()).trim();
                    meterRegistry.summary("ragreact.iterations").record(iteration);
                    return new RagReActResult(answer, steps, iteration, initialChunks);
                }

                Matcher actionMatcher = ACTION_PATTERN.matcher(completion);
                Matcher actionInputMatcher = ACTION_INPUT_PATTERN.matcher(completion);
                if (!actionMatcher.find() || !actionInputMatcher.find()) {
                    meterRegistry.summary("ragreact.iterations").record(iteration);
                    return new RagReActResult(completion.trim(), steps, iteration, initialChunks);
                }

                String thought = completion.substring(0, actionMatcher.start()).trim();
                String action = actionMatcher.group(1).trim();
                String actionInput = actionInputMatcher.group(1).trim();

                Timer.Sample toolSample = Timer.start(meterRegistry);
                ToolRegistry.Execution exec = toolRegistry.execute(action, actionInput);
                toolSample.stop(meterRegistry.timer("ragreact.tool",
                        "tool", action,
                        "outcome", exec.isError() ? "error" : "success"));
                steps.add(new AgentStep(thought, action, actionInput, exec.output(), exec.isError()));

                scratchpad.append(' ')
                        .append(completion)
                        .append("\nObservation: ").append(exec.output())
                        .append("\nThought:");
            }

            outcome = "iteration_limit";
            meterRegistry.summary("ragreact.iterations").record(maxIterations);
            return new RagReActResult(
                    "Limite de iteracoes (" + maxIterations + ") atingido sem resposta final.",
                    steps,
                    maxIterations,
                    initialChunks);
        } catch (RuntimeException e) {
            outcome = "error";
            meterRegistry.counter("ragreact.errors").increment();
            throw e;
        } finally {
            totalSample.stop(meterRegistry.timer("ragreact.run", "outcome", outcome));
        }
    }

    private String buildAugmentedQuestion(String question, List<ScoredChunk> chunks) {
        if (chunks.isEmpty()) {
            return question;
        }
        StringBuilder sb = new StringBuilder("CONTEXTO RECUPERADO (base de conhecimento):\n");
        int i = 1;
        for (ScoredChunk chunk : chunks) {
            sb.append("[").append(i++).append("] (fonte: ").append(chunk.chunk().source()).append(")\n")
                    .append(chunk.chunk().text())
                    .append("\n\n");
        }
        sb.append("---\n\nPERGUNTA: ").append(question);
        return sb.toString();
    }

    private String buildPrompt(String question, String scratchpad) {
        return PROMPT_TEMPLATE.formatted(
                toolRegistry.describeAll(),
                toolRegistry.namesList(),
                question,
                scratchpad);
    }
}
