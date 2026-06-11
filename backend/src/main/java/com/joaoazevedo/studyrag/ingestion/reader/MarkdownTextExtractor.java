package com.joaoazevedo.studyrag.ingestion.reader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import com.joaoazevedo.studyrag.error.ApiException;
import com.joaoazevedo.studyrag.ingestion.SourceType;

/**
 * Lê notas Markdown como texto UTF-8. O chunking recursivo (RN-5) é quem
 * fragmenta o conteúdo; aqui apenas garantimos extração determinística do texto.
 * Markdown não tem páginas, então os segmentos não carregam número de página.
 */
@Component
public class MarkdownTextExtractor implements DocumentTextExtractor {

    @Override
    public boolean supports(SourceType type) {
        return type == SourceType.MARKDOWN;
    }

    @Override
    public ExtractedDocument extract(Path file) {
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            return new ExtractedDocument(List.of(new ExtractedDocument.Segment(text, null)));
        } catch (IOException e) {
            // Conteúdo não decodificável como texto ⇒ não suportado (RN-1).
            throw ApiException.unsupportedType(
                    "Não foi possível ler o arquivo Markdown como texto: " + e.getMessage());
        }
    }
}
