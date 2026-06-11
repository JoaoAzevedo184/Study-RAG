package com.joaoazevedo.studyrag.ingestion;

/** Resultado da deduplicação na ingestão (RN-3). */
public enum IngestStatus {
    /** Documento processado (novo ou reindexado por mudança de conteúdo). */
    INGESTED,
    /** Mesmo sourceUri e mesmo hash: nada reprocessado. */
    SKIPPED_UNCHANGED
}
