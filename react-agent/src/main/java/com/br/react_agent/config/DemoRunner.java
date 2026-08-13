package com.br.react_agent.config;

import com.br.react_agent.agent.AgentResult;
import com.br.react_agent.agent.AgentStep;
import com.br.react_agent.agent.ReActAgentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Executa alguns exemplos no startup para demonstrar o ciclo ReAct pelo console,
 * mostrando cada passo Thought/Action/Observation. Ative com demo.run-on-startup=true.
 */
@Component
@ConditionalOnProperty(name = "demo.run-on-startup", havingValue = "true")
public class DemoRunner implements CommandLineRunner {

    private final ReActAgentService agentService;

    public DemoRunner(ReActAgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public void run(String... args) {
        List<String> exemplos = List.of(
                "Qual o clima em Fortaleza?",
                "Quanto e' (12 + 7) * 3?",
                "Converta 100 dolares em reais",
                "Quem descobriu o Brasil?"
        );

        System.out.println("\n=========== DEMO: Agente ReAct ===========");
        for (String pergunta : exemplos) {
            AgentResult result = agentService.run(pergunta);
            System.out.println("\n> Usuario: " + pergunta);
            for (AgentStep step : result.steps()) {
                System.out.println("  Thought: " + step.thought());
                System.out.println("  Action: " + step.action());
                System.out.println("  Action Input: " + step.actionInput());
                System.out.println("  Observation: " + step.observation());
            }
            System.out.println("< Final Answer: " + result.answer());
            System.out.println("  (iteracoes: " + result.iterations() + ")");
        }
        System.out.println("\n===========================================\n");
    }
}
