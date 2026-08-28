package br.uece.functioncalling.service;

import br.uece.functioncalling.llm.LlmClient;
import br.uece.functioncalling.model.ContentBlock;
import br.uece.functioncalling.model.Message;
import br.uece.functioncalling.model.ToolSpec;
import br.uece.functioncalling.tool.ToolRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Orquestrador do ciclo de Function Calling. Implementa o "agentic loop":
 *
 *   1. envia o historico + as ferramentas ao modelo;
 *   2. se a resposta contiver blocos tool_use, executa cada ferramenta
 *      localmente e devolve os resultados ao modelo;
 *   3. repete ate o modelo responder apenas com texto (sem tool_use)
 *      ou ate atingir o limite de iteracoes.
 *
 * Note que esta classe nao conhece a implementacao concreta do modelo
 * (mock ou Anthropic) nem as ferramentas especificas — apenas as abstracoes.
 */
@Service
public class ChatService {

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final MeterRegistry meterRegistry;
    private final int maxIterations;

    public ChatService(
            LlmClient llmClient,
            ToolRegistry toolRegistry,
            MeterRegistry meterRegistry,
            @Value("${llm.max-iterations:5}") int maxIterations) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.meterRegistry = meterRegistry;
        this.maxIterations = maxIterations;
    }

    public ChatResult chat(String userMessage) {
        Timer.Sample totalSample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            List<Message> history = new ArrayList<>();
            history.add(Message.user(userMessage));

            List<ToolSpec> tools = toolRegistry.specs();
            List<ToolCallTrace> trace = new ArrayList<>();

            for (int iteration = 1; iteration <= maxIterations; iteration++) {
                Message assistant = llmClient.complete(history, tools);
                history.add(assistant);

                List<ContentBlock.ToolUse> toolUses = assistant.content().stream()
                        .filter(block -> block instanceof ContentBlock.ToolUse)
                        .map(block -> (ContentBlock.ToolUse) block)
                        .toList();

                // Sem pedido de ferramenta: o modelo deu a resposta final.
                if (toolUses.isEmpty()) {
                    meterRegistry.summary("functioncalling.iterations").record(iteration);
                    return new ChatResult(extractText(assistant), trace, iteration);
                }

                // Executa cada ferramenta pedida e prepara os tool_result.
                List<ContentBlock> results = new ArrayList<>();
                for (ContentBlock.ToolUse call : toolUses) {
                    Timer.Sample toolSample = Timer.start(meterRegistry);
                    ToolRegistry.Execution exec = toolRegistry.execute(call.name(), call.input());
                    toolSample.stop(meterRegistry.timer("functioncalling.tool",
                            "tool", call.name(),
                            "outcome", exec.isError() ? "error" : "success"));
                    results.add(new ContentBlock.ToolResult(call.id(), exec.output(), exec.isError()));
                    trace.add(new ToolCallTrace(call.name(), call.input(), exec.output(), exec.isError()));
                }
                history.add(Message.toolResults(results));
            }

            outcome = "iteration_limit";
            meterRegistry.summary("functioncalling.iterations").record(maxIterations);
            return new ChatResult(
                    "Limite de iteracoes (" + maxIterations + ") atingido sem resposta final.",
                    trace,
                    maxIterations);
        } catch (RuntimeException e) {
            outcome = "error";
            meterRegistry.counter("functioncalling.errors").increment();
            throw e;
        } finally {
            totalSample.stop(meterRegistry.timer("functioncalling.chat", "outcome", outcome));
        }
    }

    private String extractText(Message message) {
        return message.content().stream()
                .filter(block -> block instanceof ContentBlock.Text)
                .map(block -> ((ContentBlock.Text) block).text())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }
}