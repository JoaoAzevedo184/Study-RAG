package com.joaoazevedo.studyrag.ingestion;

import java.util.Map;

/**
 * Entrada já validada para a ingestão (UC-1). O controller normaliza a coleção
 * (default + slug RN-4) e o tipo (RN-1) antes de chamar o serviço.
 */
public record IngestCommand(
        SourceType sourceType,
        String sourceUri,
        String collection,
        Map<String, String> metadata) {
}
