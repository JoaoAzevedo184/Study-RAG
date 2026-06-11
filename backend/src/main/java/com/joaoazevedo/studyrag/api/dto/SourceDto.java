package com.joaoazevedo.studyrag.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.joaoazevedo.studyrag.ingestion.dedup.IngestedFile;

/** Item de GET /sources (schema {@code Source}). */
public record SourceDto(
        UUID fileId,
        String sourceType,
        String sourceUri,
        String collection,
        String title,
        int chunkCount,
        String status,
        OffsetDateTime ingestedAt) {

    public static SourceDto from(IngestedFile file) {
        return new SourceDto(
                file.id(),
                file.sourceType(),
                file.sourceUri(),
                file.collection(),
                file.title(),
                file.chunkCount(),
                file.status(),
                file.ingestedAt());
    }
}
