package com.joaoazevedo.studyrag.retrieval;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import com.joaoazevedo.studyrag.ingestion.ChunkMetadata;

/**
 * Recuperação por similaridade pura no {@link VectorStore} (PgVectorStore),
 * respeitando o escopo de coleção (RN-8). A pergunta é embedada automaticamente
 * pelo {@code EmbeddingModel} configurado (Ollama nomic-embed-text); chunks abaixo
 * do limiar de similaridade são descartados na origem.
 *
 * <p>MVP: sem hybrid search e sem reranking, conforme o SDD §1.</p>
 */
@Service
public class ChunkRetriever {

    private final VectorStore vectorStore;
    private final RetrievalProperties properties;

    public ChunkRetriever(VectorStore vectorStore, RetrievalProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    /**
     * Recupera os {@code topK} chunks mais similares à {@code question}.
     *
     * @param collection quando não-nulo/em-branco, restringe a busca a essa coleção
     *                   (RN-8); nulo busca em todo o acervo.
     * @return chunks relevantes (acima do limiar), possivelmente vazio quando nada
     *         relevante é encontrado (dispara o caso "não sei" — RN-7).
     */
    public List<Document> retrieve(String question, String collection, int topK) {
        SearchRequest.Builder request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(properties.getSimilarityThreshold());

        if (collection != null && !collection.isBlank()) {
            request.filterExpression(new FilterExpressionBuilder()
                    .eq(ChunkMetadata.COLLECTION, collection)
                    .build());
        }

        List<Document> hits = vectorStore.similaritySearch(request.build());
        return hits != null ? hits : List.of();
    }
}
