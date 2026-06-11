package com.joaoazevedo.studyrag.api.dto;

import com.joaoazevedo.studyrag.ingestion.dedup.IngestedFileRepository.CollectionAggregate;

/** Item de GET /collections (schema {@code Collection}). */
public record CollectionDto(String name, int fileCount, int chunkCount) {

    public static CollectionDto from(CollectionAggregate aggregate) {
        return new CollectionDto(aggregate.name(), aggregate.fileCount(), aggregate.chunkCount());
    }
}
