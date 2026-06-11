package com.joaoazevedo.studyrag.ingestion.reader;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resultado da extração de texto de uma fonte, preservando a ordem e (quando
 * disponível) a página de cada segmento. O texto canônico — usado para o hash
 * de deduplicação (RN-3) e para o chunking (RN-5) — é a concatenação ordenada
 * dos segmentos.
 */
public record ExtractedDocument(List<Segment> segments) {

    /** Um trecho de texto e a página de origem (1-based), ou {@code null} se inaplicável. */
    public record Segment(String text, Integer page) {
    }

    /** Texto completo, juntando os segmentos por quebra de linha (determinístico). */
    public String canonicalText() {
        return segments.stream()
                .map(Segment::text)
                .collect(Collectors.joining("\n"));
    }

    public boolean isEmpty() {
        return canonicalText().isBlank();
    }
}
