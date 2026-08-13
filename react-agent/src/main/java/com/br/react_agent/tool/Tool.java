package com.br.react_agent.tool;

/**
 * Contrato de uma ferramenta executavel pelo agente ReAct.
 *
 * Diferenca central em relacao a Function Calling: ali o modelo produz argumentos
 * ja estruturados (JSON, validado contra um schema); aqui o modelo produz apenas
 * texto livre como "Action Input", e cada ferramenta e' responsavel por
 * interpretar esse texto no formato que ela mesma anuncia em {@link #description()}.
 */
public interface Tool {

    /** Nome unico da ferramenta; e' o que o modelo escreve apos "Action:". */
    String name();

    /** Descricao publicada ao modelo, incluindo o formato esperado do Action Input. */
    String description();

    /**
     * Executa a ferramenta com o texto livre produzido pelo modelo como Action Input.
     *
     * @param input texto livre, no formato descrito em {@link #description()}
     * @return saida textual a ser devolvida ao modelo como Observation
     */
    String execute(String input);
}
