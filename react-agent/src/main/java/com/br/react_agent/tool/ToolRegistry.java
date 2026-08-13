package com.br.react_agent.tool;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registro central de ferramentas. O Spring injeta automaticamente todos os beans
 * que implementam {@link Tool}, entao adicionar uma nova ferramenta e' apenas
 * criar uma classe anotada com {@code @Component} — sem tocar no agente.
 */
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools;

    public ToolRegistry(List<Tool> availableTools) {
        this.tools = availableTools.stream()
                .collect(Collectors.toMap(Tool::name, Function.identity()));
    }

    /** Nomes das ferramentas, na ordem de registro — usado na lista "[nome1, nome2, ...]" do prompt. */
    public String namesList() {
        return String.join(", ", tools.keySet());
    }

    /** Bloco "nome: descricao" de cada ferramenta, uma por linha — publicado no prompt ReAct. */
    public String describeAll() {
        return tools.values().stream()
                .map(tool -> "- " + tool.name() + ": " + tool.description())
                .collect(Collectors.joining("\n"));
    }

    public boolean has(String name) {
        return tools.containsKey(name);
    }

    /**
     * Executa a ferramenta pelo nome, capturando erros como saida textual
     * (para que o modelo possa reagir ao erro em vez de a aplicacao quebrar).
     */
    public Execution execute(String name, String input) {
        Tool tool = tools.get(name);
        if (tool == null) {
            return new Execution("Ferramenta '" + name + "' nao encontrada. Ferramentas disponiveis: "
                    + namesList() + ".", true);
        }
        try {
            return new Execution(tool.execute(input), false);
        } catch (Exception e) {
            return new Execution("Erro ao executar '" + name + "': " + e.getMessage(), true);
        }
    }

    /** Resultado de uma execucao: saida textual e flag de erro. */
    public record Execution(String output, boolean isError) {}
}
