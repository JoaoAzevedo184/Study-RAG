package com.joaoazevedo.studyrag.ingestion;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

/**
 * Encapsula as operações no vector store usadas pela ingestão e pela remoção:
 * gravar chunks (gera embeddings via Ollama automaticamente) e apagar todos os
 * chunks de uma fonte por {@code source_uri} (RN-3 reindexação e RN-9 cascata).
 */
@Component
public class VectorChunkWriter {

    private final VectorStore vectorStore;

    public VectorChunkWriter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /** Grava os chunks (com seus metadados) no vector store. */
    public void write(List<Document> chunks) {
        if (!chunks.isEmpty()) {
            vectorStore.add(chunks);
        }
    }

    /** Remove todos os chunks cujo metadado {@code source_uri} é o informado. */
    public void deleteBySourceUri(String sourceUri) {
        var filter = new FilterExpressionBuilder()
                .eq(ChunkMetadata.SOURCE_URI, sourceUri)
                .build();
        vectorStore.delete(filter);
    }
}
