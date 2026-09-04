package com.example.ragreact.llm;

import java.util.List;

/**
 * Abstracao do modelo de linguagem para o ciclo ReAct. Diferente de Function
 * Calling, aqui nao existe um formato estruturado de "tool_use": o modelo apenas
 * continua um texto (o "scratchpad" ReAct) e a aplicacao interpreta esse texto
 * para descobrir qual acao foi decidida.
 *
 * {@code stopSequences} evita que o modelo alucine o resultado da ferramenta:
 * pedimos para ele parar assim que escrever "\nObservation:", pois esse trecho
 * quem preenche e' a aplicacao, apos executar a ferramenta de verdade.
 */
public interface LlmClient {

    String complete(String prompt, List<String> stopSequences);
}
