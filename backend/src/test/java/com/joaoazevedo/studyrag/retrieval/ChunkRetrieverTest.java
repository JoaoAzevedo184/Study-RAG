package com.joaoazevedo.studyrag.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * Cobre o escopo por coleção da recuperação (RN-8): o filtro é aplicado quando há
 * coleção e omitido quando a busca é global, além dos parâmetros de busca (topK,
 * limiar de similaridade).
 */
class ChunkRetrieverTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final RetrievalProperties properties = new RetrievalProperties();
    private final ChunkRetriever retriever = new ChunkRetriever(vectorStore, properties);

    @Test
    void appliesCollectionFilterAndSearchParameters() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        retriever.retrieve("o que é chunking?", "docs", 5);

        SearchRequest request = captureRequest();
        assertThat(request.getQuery()).isEqualTo("o que é chunking?");
        assertThat(request.getTopK()).isEqualTo(5);
        assertThat(request.getSimilarityThreshold()).isEqualTo(properties.getSimilarityThreshold());
        assertThat(request.getFilterExpression())
                .as("coleção informada deve restringir a busca (RN-8)")
                .isNotNull();
    }

    @Test
    void omitsFilterForGlobalSearch() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        retriever.retrieve("busca global", null, 5);

        assertThat(captureRequest().getFilterExpression())
                .as("sem coleção, busca em todo o acervo")
                .isNull();
    }

    @Test
    void treatsBlankCollectionAsGlobalSearch() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        retriever.retrieve("busca global", "   ", 5);

        assertThat(captureRequest().getFilterExpression()).isNull();
    }

    private SearchRequest captureRequest() {
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        return captor.getValue();
    }
}
