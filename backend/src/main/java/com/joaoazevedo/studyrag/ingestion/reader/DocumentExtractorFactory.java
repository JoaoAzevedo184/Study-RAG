package com.joaoazevedo.studyrag.ingestion.reader;

import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import com.joaoazevedo.studyrag.error.ApiException;
import com.joaoazevedo.studyrag.ingestion.SourceType;

/**
 * Seleciona o {@link DocumentTextExtractor} adequado ao {@link SourceType} e
 * delega a extração. Novos tipos (web, vídeo) entram registrando outro extrator.
 */
@Component
public class DocumentExtractorFactory {

    private final List<DocumentTextExtractor> extractors;

    public DocumentExtractorFactory(List<DocumentTextExtractor> extractors) {
        this.extractors = extractors;
    }

    public ExtractedDocument extract(SourceType type, Path file) {
        return extractors.stream()
                .filter(e -> e.supports(type))
                .findFirst()
                .orElseThrow(() -> ApiException.unsupportedType("Tipo de fonte não suportado: " + type))
                .extract(file);
    }
}
