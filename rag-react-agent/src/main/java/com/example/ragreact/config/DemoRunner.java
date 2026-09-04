package com.example.ragreact.config;

import com.example.ragreact.agent.AgentStep;
import com.example.ragreact.agent.RagReActAgentService;
import com.example.ragreact.agent.RagReActResult;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Executa alguns exemplos no startup para demonstrar o ciclo RAG + ReAct pelo
 * console, mostrando cada passo Thought/Action/Observation. Ative com demo.run-on-startup=true.
 */
@Component
@ConditionalOnProperty(name = "demo.run-on-startup", havingValue = "true")
public class DemoRunner implements CommandLineRunner {

    private final RagReActAgentService agentService;

    public DemoRunner(RagReActAgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public void run(String... args) {
        List<String> exemplos = List.of(
                "Quais documentos sao aceitos para a isencao da taxa do vestibular?",
                "Quanto e' (12 + 7) * 3?",
                "Qual o clima em Fortaleza?"
        );

        System.out.println("\n=========== DEMO: RAG + ReAct ===========");
        for (String pergunta : exemplos) {
            RagReActResult result = agentService.run(pergunta);
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
