package com.joaoazevedo.studyrag.ingestion;

import java.util.UUID;

/** Resultado da ingestão devolvido ao controller (mapeia para IngestResponse). */
public record IngestionResult(UUID fileId, IngestStatus status, int chunkCount) {
}
