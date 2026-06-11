package com.joaoazevedo.studyrag.ingestion.chunking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class RecursiveCharacterChunkerTest {

    private final RecursiveCharacterChunker chunker = new RecursiveCharacterChunker(1000, 200);

    @Test
    void blankOrNullProducesNoChunks() {
        assertThat(chunker.chunk(null)).isEmpty();
        assertThat(chunker.chunk("   \n  ")).isEmpty();
    }

    @Test
    void shortTextStaysAsSingleChunk() {
        String text = "Um parágrafo curto sobre chunking recursivo.";
        assertThat(chunker.chunk(text)).containsExactly(text);
    }

    @Test
    void chunkingIsDeterministic() {
        String text = buildLongText();
        List<String> first = chunker.chunk(text);
        List<String> second = chunker.chunk(text);
        assertThat(first).isEqualTo(second);
        assertThat(first).hasSizeGreaterThan(1);
    }

    @Test
    void everyChunkRespectsTargetSize() {
        List<String> chunks = chunker.chunk(buildLongText());
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.length()).isLessThanOrEqualTo(1000));
    }

    @Test
    void consecutiveChunksOverlap() {
        // Com overlap de 200, chunks adjacentes compartilham conteúdo de fronteira.
        List<String> chunks = chunker.chunk(buildLongText());
        boolean someOverlap = false;
        for (int i = 0; i < chunks.size() - 1; i++) {
            String tail = suffix(chunks.get(i), 50);
            if (chunks.get(i + 1).contains(tail.strip().split(" ")[0])) {
                someOverlap = true;
                break;
            }
        }
        assertThat(someOverlap).isTrue();
    }

    private String suffix(String s, int n) {
        return s.length() <= n ? s : s.substring(s.length() - n);
    }

    private String buildLongText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            sb.append("Chunking recursivo divide o texto em fragmentos sobrepostos de tamanho alvo. ");
            if (i % 5 == 0) {
                sb.append("\n\n");
            }
        }
        return sb.toString();
    }
}
