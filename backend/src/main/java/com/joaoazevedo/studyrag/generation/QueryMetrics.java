package com.joaoazevedo.studyrag.generation;

/**
 * Métricas de execução da consulta (schema {@code QueryMetrics} do OpenAPI).
 *
 * @param retrievalMs     tempo de recuperação dos chunks, em milissegundos.
 * @param generationMs    tempo de geração da resposta, em milissegundos (0 no caso "não sei").
 * @param tokensUsed      tokens consumidos pelo modelo de geração (0 quando não há geração).
 * @param chunksRetrieved número de chunks relevantes recuperados.
 */
public record QueryMetrics(
        long retrievalMs,
        long generationMs,
        int tokensUsed,
        int chunksRetrieved) {
}
