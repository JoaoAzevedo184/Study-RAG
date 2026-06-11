package com.joaoazevedo.studyrag.generation;

/**
 * Saída crua da etapa de geração: o texto da resposta e os tokens consumidos pelo
 * modelo (para a métrica {@code tokensUsed}).
 */
public record GeneratedAnswer(String answer, int tokensUsed) {
}
