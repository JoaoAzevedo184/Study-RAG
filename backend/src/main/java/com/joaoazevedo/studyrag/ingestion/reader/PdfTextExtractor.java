package com.joaoazevedo.studyrag.ingestion.reader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import com.joaoazevedo.studyrag.error.ApiException;
import com.joaoazevedo.studyrag.ingestion.SourceType;

/**
 * Extrai texto de PDF página-a-página, preservando o número da página para as
 * citações (RN-7). Usa o {@code PagePdfDocumentReader} do Spring AI (PDFBox).
 */
@Component
public class PdfTextExtractor implements DocumentTextExtractor {

    // Chaves de metadado de página usadas pelo reader (lookup defensivo).
    private static final List<String> PAGE_KEYS = List.of("page_number", "page");

    @Override
    public boolean supports(SourceType type) {
        return type == SourceType.PDF;
    }

    @Override
    public ExtractedDocument extract(Path file) {
        try {
            var reader = new PagePdfDocumentReader(
                    new FileSystemResource(file.toFile()),
                    PdfDocumentReaderConfig.builder()
                            .withPagesPerDocument(1)
                            .build());

            List<ExtractedDocument.Segment> segments = new ArrayList<>();
            for (Document doc : reader.get()) {
                String text = doc.getText();
                if (text != null && !text.isBlank()) {
                    segments.add(new ExtractedDocument.Segment(text, pageOf(doc)));
                }
            }
            return new ExtractedDocument(segments);
        } catch (RuntimeException e) {
            throw ApiException.unsupportedType(
                    "Não foi possível extrair texto do PDF: " + e.getMessage());
        }
    }

    private Integer pageOf(Document doc) {
        for (String key : PAGE_KEYS) {
            Object value = doc.getMetadata().get(key);
            if (value instanceof Number n) {
                return n.intValue();
            }
        }
        return null;
    }
}
