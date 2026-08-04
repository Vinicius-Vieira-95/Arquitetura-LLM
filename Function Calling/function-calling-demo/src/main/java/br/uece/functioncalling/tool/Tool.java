package br.uece.functioncalling.tool;

import br.uece.functioncalling.model.ToolSpec;
import java.util.Map;

/**
 * Contrato de uma ferramenta executavel pela aplicacao.
 *
 * O principio central de Function Calling: o modelo NUNCA executa a ferramenta.
 * Ele apenas escolhe qual chamar e produz os argumentos; quem executa e' o
 * codigo da aplicacao, por meio de {@link #execute(Map)}. A {@link #spec()} e'
 * o que o modelo enxerga (nome, descricao e schema dos parametros).
 */
public interface Tool {

    /** Nome unico da ferramenta; deve casar com {@code spec().name()}. */
    String name();

    /** Especificacao publicada ao modelo. */
    ToolSpec spec();

    /**
     * Executa a ferramenta com os argumentos escolhidos pelo modelo.
     *
     * @param input argumentos ja estruturados
     * @return saida textual a ser devolvida ao modelo como tool_result
     */
    String execute(Map<String, Object> input);
}
