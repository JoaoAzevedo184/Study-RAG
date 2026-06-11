package com.joaoazevedo.studyrag.generation;

/**
 * Fonte citada que embasou a resposta (schema {@code SourceCitation} do OpenAPI).
 * Derivada diretamente de um chunk recuperado (RN-7).
 *
 * @param sourceUri  caminho/identificador da fonte de origem.
 * @param title      título legível da fonte, quando disponível.
 * @param collection coleção à qual o chunk pertence.
 * @param page       página de origem (PDF); {@code null} em Markdown.
 * @param snippet    trecho do chunk que embasou a resposta.
 * @param score      similaridade (cosseno normalizado, 0..1).
 */
public record Citation(
        String sourceUri,
        String title,
        String collection,
        Integer page,
        String snippet,
        double score) {
}
