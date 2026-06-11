package com.joaoazevedo.studyrag.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import com.joaoazevedo.studyrag.error.ApiException;
import com.joaoazevedo.studyrag.error.ErrorCode;
import com.joaoazevedo.studyrag.ingestion.ChunkMetadata;
import com.joaoazevedo.studyrag.ingestion.CollectionService;
import com.joaoazevedo.studyrag.retrieval.ChunkRetriever;
import com.joaoazevedo.studyrag.retrieval.RetrievalProperties;

/**
 * Cobre os comportamentos críticos da consulta: o caso "não sei" (RN-7, sources
 * vazio) e o escopo por coleção (RN-8, inclusive o 422 de coleção inexistente).
 */
class QueryServiceImplTest {

    private final ChunkRetriever retriever = mock(ChunkRetriever.class);
    private final AnswerGenerator generator = mock(AnswerGenerator.class);
    private final CollectionService collectionService = mock(CollectionService.class);
    private final RetrievalProperties retrievalProperties = new RetrievalProperties();
    private final GenerationProperties generationProperties = new GenerationProperties();

    private final QueryServiceImpl service = new QueryServiceImpl(
            retriever, generator, collectionService, retrievalProperties, generationProperties);

    @Test
    void noRelevantChunksAnswersDontKnowWithEmptySources() {
        when(collectionService.exists("docs")).thenReturn(true);
        when(retriever.retrieve("Como fazer um risoto?", "docs", 5)).thenReturn(List.of());

        QueryResult result = service.query(new QueryCommand("Como fazer um risoto?", "docs", null));

        assertThat(result.sources()).isEmpty();
        assertThat(result.answer()).isEqualTo(generationProperties.getNoAnswerMessage());
        assertThat(result.metrics().chunksRetrieved()).isZero();
        assertThat(result.metrics().tokensUsed()).isZero();
        assertThat(result.metrics().generationMs()).isZero();
        verify(generator, never()).generate(any(), anyList());
    }

    @Test
    void unknownCollectionThrowsCollectionNotFound() {
        when(collectionService.exists("inexistente")).thenReturn(false);

        assertThatThrownBy(() -> service.query(new QueryCommand("pergunta válida", "inexistente", null)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.COLLECTION_NOT_FOUND));

        verifyNoInteractions(retriever, generator);
    }

    @Test
    void restrictsRetrievalToRequestedCollectionAndCitesOnlyIt() {
        when(collectionService.exists("spring")).thenReturn(true);
        Document hit = new Document("Injeção de dependência no Spring.", Map.of(
                ChunkMetadata.SOURCE_URI, "/uploads/spring.pdf",
                ChunkMetadata.COLLECTION, "spring",
                ChunkMetadata.TITLE, "Guia Spring",
                ChunkMetadata.PAGE, 7));
        when(retriever.retrieve("O que é DI?", "spring", 3)).thenReturn(List.of(hit));
        when(generator.generate(eq("O que é DI?"), anyList()))
                .thenReturn(new GeneratedAnswer("DI é inversão de controle.", 42));

        QueryResult result = service.query(new QueryCommand("O que é DI?", "spring", 3));

        assertThat(result.answer()).isEqualTo("DI é inversão de controle.");
        assertThat(result.sources()).singleElement().satisfies(c -> {
            assertThat(c.collection()).isEqualTo("spring");
            assertThat(c.sourceUri()).isEqualTo("/uploads/spring.pdf");
            assertThat(c.title()).isEqualTo("Guia Spring");
            assertThat(c.page()).isEqualTo(7);
            assertThat(c.snippet()).isEqualTo("Injeção de dependência no Spring.");
        });
        assertThat(result.sources()).noneMatch(c -> "django".equals(c.collection()));
        assertThat(result.metrics().chunksRetrieved()).isEqualTo(1);
        assertThat(result.metrics().tokensUsed()).isEqualTo(42);
        verify(retriever).retrieve("O que é DI?", "spring", 3);
    }
}
