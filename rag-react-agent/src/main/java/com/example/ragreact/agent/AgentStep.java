package com.example.ragreact.agent;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Um passo Thought/Action/Action Input/Observation do ciclo ReAct.
 * Serve para expor o "caminho de raciocinio" percorrido pelo agente — util
 * para observabilidade e para fins didaticos.
 */
public record AgentStep(
        @Schema(description = "Raciocinio do modelo antes de decidir a acao") String thought,
        @Schema(description = "Nome da ferramenta escolhida") String action,
        @Schema(description = "Entrada em texto livre passada a ferramenta") String actionInput,
        @Schema(description = "Resultado devolvido pela ferramenta") String observation,
        @Schema(description = "Indica se a execucao da ferramenta falhou") boolean isError) {}
