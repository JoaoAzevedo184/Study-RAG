package com.joaoazevedo.studyrag.api.dto;

import java.util.UUID;

import com.joaoazevedo.studyrag.ingestion.IngestionResult;

/** Resposta de POST /ingest (schema {@code IngestResponse}). */
public record IngestResponseDto(UUID fileId, String status, int chunkCount) {

    public static IngestResponseDto from(IngestionResult result) {
        return new IngestResponseDto(result.fileId(), result.status().name(), result.chunkCount());
    }
}
