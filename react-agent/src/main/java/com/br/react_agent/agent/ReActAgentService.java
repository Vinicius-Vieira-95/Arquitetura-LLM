package com.br.react_agent.agent;

import com.br.react_agent.llm.LlmClient;
import com.br.react_agent.tool.ToolRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orquestrador do ciclo ReAct (Reasoning + Acting), o padrao descrito por
 * Yao et al. (2022). Diferente de Function Calling — onde o modelo devolve um
 * bloco estruturado tool_use —, aqui o modelo apenas continua um texto livre
 * seguindo um formato fixo, e a APLICACAO e' quem interpreta esse texto:
 *
 *   Thought: <raciocinio do modelo sobre o que fazer>
 *   Action: <nome de uma ferramenta>
 *   Action Input: <texto livre para a ferramenta>
 *   Observation: <preenchido pela aplicacao, apos executar a ferramenta de verdade>
 *   ... (o padrao Thought/Action/Action Input/Observation se repete)
 *   Thought: <raciocinio final>
 *   Final Answer: <resposta ao usuario>
 *
 * O laco:
 *   1. monta o prompt (instrucoes + "scratchpad" acumulado ate agora);
 *   2. pede ao modelo para continuar o texto, parando antes de "Observation:"
 *      (via stop sequence) — assim o modelo nunca inventa o resultado da ferramenta;
 *   3. se a continuacao contiver "Final Answer:", o ciclo termina;
 *   4. caso contrario, extrai Action/Action Input, executa a ferramenta de verdade
 *      e anexa "Observation: <resultado>\nThought:" ao scratchpad, repetindo o laco.
 */
@Service
public class ReActAgentService {

    private static final String STOP_SEQUENCE = "\nObservation:";
    private static final Pattern ACTION_PATTERN = Pattern.compile("Action:\\s*(.+)");
    private static final Pattern ACTION_INPUT_PATTERN = Pattern.compile("Action Input:\\s*(.+)", Pattern.DOTALL);
    private static final String FINAL_ANSWER_MARKER = "Final Answer:";

    private static final String PROMPT_TEMPLATE = """
            Voce e' um agente que resolve tarefas usando o padrao ReAct (Reasoning + Acting).
            Responda a pergunta do usuario da melhor forma possivel. Voce tem acesso as seguintes ferramentas:

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

            Importante: voce esta continuando uma transcricao ja iniciada. Responda APENAS com o texto que \
            continua a transcricao a partir do ponto em que ela para (que termina em "Thought:") — nao repita \
            a palavra "Thought", nao repita nada do que ja foi escrito, e pare antes de escrever "Observation:" \
            (o resultado da acao sera fornecido a voce na proxima rodada).

            Comece!

            Question: %s
            Thought:%s""";

    private final LlmClient llmClient;
    private final ToolRegistry toolRegistry;
    private final int maxIterations;

    public ReActAgentService(
            LlmClient llmClient,
            ToolRegistry toolRegistry,
            @Value("${react.max-iterations:5}") int maxIterations) {
        this.llmClient = llmClient;
        this.toolRegistry = toolRegistry;
        this.maxIterations = maxIterations;
    }

    public AgentResult run(String question) {
        StringBuilder scratchpad = new StringBuilder();
        List<AgentStep> steps = new ArrayList<>();

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            String prompt = buildPrompt(question, scratchpad.toString());
            String completion = llmClient.complete(prompt, List.of(STOP_SEQUENCE)).stripTrailing();

            int finalAnswerIdx = completion.indexOf(FINAL_ANSWER_MARKER);
            if (finalAnswerIdx >= 0) {
                String answer = completion.substring(finalAnswerIdx + FINAL_ANSWER_MARKER.length()).trim();
                return new AgentResult(answer, steps, iteration);
            }

            Matcher actionMatcher = ACTION_PATTERN.matcher(completion);
            Matcher actionInputMatcher = ACTION_INPUT_PATTERN.matcher(completion);
            if (!actionMatcher.find() || !actionInputMatcher.find()) {
                // O modelo nao seguiu o formato esperado: trata a saida como resposta final.
                return new AgentResult(completion.trim(), steps, iteration);
            }

            String thought = completion.substring(0, actionMatcher.start()).trim();
            String action = actionMatcher.group(1).trim();
            String actionInput = actionInputMatcher.group(1).trim();

            ToolRegistry.Execution exec = toolRegistry.execute(action, actionInput);
            steps.add(new AgentStep(thought, action, actionInput, exec.output(), exec.isError()));

            scratchpad.append(' ')
                    .append(completion)
                    .append("\nObservation: ").append(exec.output())
                    .append("\nThought:");
        }

        return new AgentResult(
                "Limite de iteracoes (" + maxIterations + ") atingido sem resposta final.",
                steps,
                maxIterations);
    }

    private String buildPrompt(String question, String scratchpad) {
        return PROMPT_TEMPLATE.formatted(
                toolRegistry.describeAll(),
                toolRegistry.namesList(),
                question,
                scratchpad);
    }
}
