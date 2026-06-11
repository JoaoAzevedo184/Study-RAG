package com.joaoazevedo.studyrag.generation;

import java.util.List;

/**
 * Resultado de uma consulta (schema {@code QueryResponse} do OpenAPI): resposta
 * fundamentada, fontes citadas e métricas. No caso "não sei" (RN-7), {@code sources}
 * é uma lista vazia.
 */
public record QueryResult(String answer, List<Citation> sources, QueryMetrics metrics) {
}
