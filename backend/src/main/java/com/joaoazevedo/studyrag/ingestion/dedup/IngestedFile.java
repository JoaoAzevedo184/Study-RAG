package com.joaoazevedo.studyrag.ingestion.dedup;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Registro de uma fonte ingerida (tabela {@code ingested_files}). Base da
 * deduplicação por hash (RN-3) e da unicidade de {@code sourceUri} (RN-2).
 */
public record IngestedFile(
        UUID id,
        String sourceType,
        String sourceUri,
        String collection,
        String contentHash,
        String title,
        int chunkCount,
        String status,
        OffsetDateTime ingestedAt,
        OffsetDateTime updatedAt) {
}
