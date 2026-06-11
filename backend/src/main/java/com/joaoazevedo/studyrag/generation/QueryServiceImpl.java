package com.joaoazevedo.studyrag.generation;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import com.joaoazevedo.studyrag.error.ApiException;
import com.joaoazevedo.studyrag.error.ErrorCode;
import com.joaoazevedo.studyrag.ingestion.ChunkMetadata;
import com.joaoazevedo.studyrag.ingestion.CollectionService;
import com.joaoazevedo.studyrag.retrieval.ChunkRetriever;
import com.joaoazevedo.studyrag.retrieval.RetrievalProperties;

/**
 * Orquestra a consulta (UC-5): valida o escopo de coleção (RN-8), recupera os chunks
 * relevantes, decide o caso "não sei" (RN-7) e, havendo contexto, gera a resposta
 * fundamentada (RN-6), montando o {@link QueryResult} com fontes e métricas.
 */
@Service
public class QueryServiceImpl implements QueryService {

    private final ChunkRetriever retriever;
    private final AnswerGenerator generator;
    private final CollectionService collectionService;
    private final RetrievalProperties retrievalProperties;
    private final GenerationProperties generationProperties;

    public QueryServiceImpl(ChunkRetriever retriever,
                            AnswerGenerator generator,
                            CollectionService collectionService,
                            RetrievalProperties retrievalProperties,
                            GenerationProperties generationProperties) {
        this.retriever = retriever;
        this.generator = generator;
        this.collectionService = collectionService;
        this.retrievalProperties = retrievalProperties;
        this.generationProperties = generationProperties;
    }

    @Override
    public QueryResult query(QueryCommand command) {
        // RN-8: coleção informada precisa existir (e ter conteúdo) — senão 422.
        if (command.collection() != null && !collectionService.exists(command.collection())) {
            throw new ApiException(ErrorCode.COLLECTION_NOT_FOUND,
                    "Coleção não encontrada ou vazia: " + command.collection());
        }

        int topK = command.topK() != null ? command.topK() : retrievalProperties.getDefaultTopK();

        long retrievalStart = System.nanoTime();
        List<Document> chunks = retriever.retrieve(command.question(), command.collection(), topK);
        long retrievalMs = elapsedMs(retrievalStart);

        // RN-7: nenhum chunk relevante ⇒ "não sei" com sources vazio (status 200, não é erro).
        if (chunks.isEmpty()) {
            return new QueryResult(
                    generationProperties.getNoAnswerMessage(),
                    List.of(),
                    new QueryMetrics(retrievalMs, 0, 0, 0));
        }

        long generationStart = System.nanoTime();
        GeneratedAnswer generated = generator.generate(command.question(), chunks);
        long generationMs = elapsedMs(generationStart);

        return new QueryResult(
                generated.answer(),
                chunks.stream().map(QueryServiceImpl::toCitation).toList(),
                new QueryMetrics(retrievalMs, generationMs, generated.tokensUsed(), chunks.size()));
    }

    /** Materializa um {@link Citation} a partir dos metadados do chunk (RN-7). */
    private static Citation toCitation(Document doc) {
        var metadata = doc.getMetadata();
        Object page = metadata.get(ChunkMetadata.PAGE);
        Double score = doc.getScore();
        return new Citation(
                asString(metadata.get(ChunkMetadata.SOURCE_URI)),
                asString(metadata.get(ChunkMetadata.TITLE)),
                asString(metadata.get(ChunkMetadata.COLLECTION)),
                page instanceof Number n ? n.intValue() : null,
                doc.getText(),
                score != null ? score : 0.0);
    }

    private static String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
