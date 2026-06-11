package com.joaoazevedo.studyrag.ingestion.reader;

import java.nio.file.Path;

import com.joaoazevedo.studyrag.ingestion.SourceType;

/**
 * Extrator de texto por tipo de fonte (padrão ETL: Reader). Implementações
 * devem lançar {@link com.joaoazevedo.studyrag.error.ApiException} com
 * {@code UNSUPPORTED_TYPE} (415) quando o conteúdo não puder ser extraído (RN-1).
 */
public interface DocumentTextExtractor {

    boolean supports(SourceType type);

    ExtractedDocument extract(Path file);
}
