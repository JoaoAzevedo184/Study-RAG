package com.joaoazevedo.studyrag.ingestion.chunking;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Component;

import com.joaoazevedo.studyrag.config.IngestionProperties;

/**
 * Chunking recursivo por caracteres (RN-5): tamanho-alvo e overlap fixos,
 * separadores em ordem decrescente de granularidade
 * ({@code "\n\n"}, {@code "\n"}, {@code " "}, {@code ""}). É <b>determinístico</b>:
 * a mesma entrada produz sempre os mesmos chunks, garantindo reprodutibilidade.
 *
 * <p>Implementa a estratégia split-recursivo + merge-com-overlap (mesma usada
 * por bibliotecas como LangChain), sem dependências externas.</p>
 */
@Component
public class RecursiveCharacterChunker {

    private static final List<String> SEPARATORS = List.of("\n\n", "\n", " ", "");

    private final int chunkSize;
    private final int overlap;

    public RecursiveCharacterChunker(IngestionProperties properties) {
        this(properties.getChunk().getSize(), properties.getChunk().getOverlap());
    }

    public RecursiveCharacterChunker(int chunkSize, int overlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize deve ser positivo");
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("overlap deve estar em [0, chunkSize)");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    /** Fragmenta o texto em chunks determinísticos. Texto vazio/branco ⇒ lista vazia. */
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return splitRecursive(text, SEPARATORS);
    }

    private List<String> splitRecursive(String text, List<String> separators) {
        // Escolhe o separador mais grosso que ainda ocorre no texto.
        String separator = separators.get(separators.size() - 1);
        List<String> nextSeparators = List.of();
        for (int i = 0; i < separators.size(); i++) {
            String sep = separators.get(i);
            if (sep.isEmpty() || text.contains(sep)) {
                separator = sep;
                nextSeparators = separators.subList(i + 1, separators.size());
                break;
            }
        }

        List<String> pieces = splitBySeparator(text, separator);

        List<String> result = new ArrayList<>();
        List<String> buffer = new ArrayList<>();
        for (String piece : pieces) {
            if (piece.length() < chunkSize) {
                buffer.add(piece);
            } else {
                if (!buffer.isEmpty()) {
                    result.addAll(mergeSplits(buffer, separator));
                    buffer.clear();
                }
                if (nextSeparators.isEmpty()) {
                    // Não há separador mais fino: emite o pedaço como está.
                    result.add(piece);
                } else {
                    result.addAll(splitRecursive(piece, nextSeparators));
                }
            }
        }
        if (!buffer.isEmpty()) {
            result.addAll(mergeSplits(buffer, separator));
        }
        return result;
    }

    private List<String> splitBySeparator(String text, String separator) {
        List<String> out = new ArrayList<>();
        if (separator.isEmpty()) {
            for (int i = 0; i < text.length(); i++) {
                out.add(String.valueOf(text.charAt(i)));
            }
            return out;
        }
        int from = 0;
        int idx;
        while ((idx = text.indexOf(separator, from)) >= 0) {
            if (idx > from) {
                out.add(text.substring(from, idx));
            }
            from = idx + separator.length();
        }
        if (from < text.length()) {
            out.add(text.substring(from));
        }
        return out;
    }

    /** Une pedaços pequenos respeitando o tamanho-alvo e mantendo overlap entre chunks. */
    private List<String> mergeSplits(List<String> splits, String separator) {
        int sepLen = separator.length();
        List<String> chunks = new ArrayList<>();
        Deque<String> current = new ArrayDeque<>();
        int total = 0;

        for (String piece : splits) {
            int pieceLen = piece.length();
            if (total + pieceLen + (current.isEmpty() ? 0 : sepLen) > chunkSize && !current.isEmpty()) {
                String chunk = String.join(separator, current);
                if (!chunk.isBlank()) {
                    chunks.add(chunk);
                }
                // Reduz a janela até caber o overlap desejado.
                while (total > overlap
                        || (total + pieceLen + (current.isEmpty() ? 0 : sepLen) > chunkSize && total > 0)) {
                    String removed = current.removeFirst();
                    total -= removed.length() + (current.isEmpty() ? 0 : sepLen);
                }
            }
            current.addLast(piece);
            total += pieceLen + (current.size() > 1 ? sepLen : 0);
        }
        String chunk = String.join(separator, current);
        if (!chunk.isBlank()) {
            chunks.add(chunk);
        }
        return chunks;
    }
}
