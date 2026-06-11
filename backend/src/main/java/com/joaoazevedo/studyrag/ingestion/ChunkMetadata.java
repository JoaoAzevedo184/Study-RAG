package com.joaoazevedo.studyrag.ingestion;

/**
 * Chaves de metadados gravadas em cada chunk no vector store. São o ponto de
 * acoplamento com os pacotes {@code retrieval} (filtro por coleção — RN-8) e
 * {@code generation} (montagem das citações — RN-7). NÃO renomear sem alinhar
 * com o outro agente.
 */
public final class ChunkMetadata {

    /** Caminho/identificador da fonte de origem (igual ao {@code sourceUri}). */
    public static final String SOURCE_URI = "source_uri";

    /** Coleção temática à qual o chunk pertence (RN-8). */
    public static final String COLLECTION = "collection";

    /** Página de origem (1-based) quando aplicável (PDF); ausente em Markdown. */
    public static final String PAGE = "page";

    /** Título legível da fonte, quando disponível. */
    public static final String TITLE = "title";

    private ChunkMetadata() {
    }
}
