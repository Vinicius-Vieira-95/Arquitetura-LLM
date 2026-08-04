package br.uece.functioncalling.config;

import br.uece.functioncalling.service.ChatResult;
import br.uece.functioncalling.service.ChatService;
import br.uece.functioncalling.service.ToolCallTrace;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Executa alguns exemplos no startup para demonstrar o ciclo de Function Calling
 * pelo console, sem necessidade de HTTP. Ative com demo.run-on-startup=true.
 */
@Component
@ConditionalOnProperty(name = "demo.run-on-startup", havingValue = "true")
public class DemoRunner implements CommandLineRunner {

    private final ChatService chatService;

    public DemoRunner(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void run(String... args) {
        List<String> exemplos = List.of(
                "Qual o clima em Fortaleza?",
                "Quanto e' (12 + 7) * 3?",
                "Converta 100 dolares em reais",
                "Quem descobriu o Brasil?"
        );

        System.out.println("\n=========== DEMO: Function Calling ===========");
        for (String pergunta : exemplos) {
            ChatResult result = chatService.chat(pergunta);
            System.out.println("\n> Usuario: " + pergunta);
            for (ToolCallTrace call : result.toolCalls()) {
                System.out.printf("  [ferramenta] %s(%s) -> %s%n",
                        call.tool(), call.input(), call.output());
            }
            System.out.println("< Assistente: " + result.answer());
            System.out.println("  (iteracoes: " + result.iterations() + ")");
        }
        System.out.println("\n==============================================\n");
    }
}
