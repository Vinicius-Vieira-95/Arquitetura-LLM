package br.uece.functioncalling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicacao Spring Boot.
 *
 * {@code scanBasePackages = "br.uece"} inclui o modulo compartilhado
 * {@code br.uece.llm} (ToolRegistry, ferramentas e AnthropicLlmClient),
 * que fica fora do pacote padrao de scan ({@code br.uece.functioncalling}).
 */
@SpringBootApplication(scanBasePackages = "br.uece")
public class FunctionCallingApplication {

    public static void main(String[] args) {
        SpringApplication.run(FunctionCallingApplication.class, args);
    }
}
