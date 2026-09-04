package br.uece.llm.tool;

import br.uece.llm.model.ToolSpec;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registro central de ferramentas. O Spring injeta automaticamente todos os beans
 * que implementam {@link Tool}, de modo que adicionar uma nova ferramenta e' apenas
 * criar uma classe anotada com {@code @Component} — sem tocar no orquestrador.
 */
@Component
public class ToolRegistry {

    private final Map<String, Tool> tools;

    public ToolRegistry(List<Tool> availableTools) {
        this.tools = availableTools.stream()
                .collect(Collectors.toMap(Tool::name, Function.identity()));
    }

    /** Especificacoes de todas as ferramentas, para enviar ao modelo. */
    public List<ToolSpec> specs() {
        return tools.values().stream().map(Tool::spec).toList();
    }

    public boolean has(String name) {
        return tools.containsKey(name);
    }

    /**
     * Executa a ferramenta pelo nome, capturando erros como saida textual
     * (para que o modelo possa reagir ao erro em vez de a aplicacao quebrar).
     *
     * @return par (saida, houveErro)
     */
    public Execution execute(String name, Map<String, Object> input) {
        Tool tool = tools.get(name);
        if (tool == null) {
            return new Execution("Ferramenta '" + name + "' nao encontrada.", true);
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
