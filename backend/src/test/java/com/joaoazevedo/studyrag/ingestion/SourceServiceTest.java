package com.joaoazevedo.studyrag.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.joaoazevedo.studyrag.error.ApiException;
import com.joaoazevedo.studyrag.error.ErrorCode;
import com.joaoazevedo.studyrag.ingestion.dedup.IngestedFile;
import com.joaoazevedo.studyrag.ingestion.dedup.IngestedFileRepository;

/** Cobre a remoção em cascata (RN-9) e o erro de fonte inexistente. */
class SourceServiceTest {

    private final IngestedFileRepository repository = mock(IngestedFileRepository.class);
    private final VectorChunkWriter vectorWriter = mock(VectorChunkWriter.class);
    private final SourceService service = new SourceService(repository, vectorWriter);

    @Test
    void deleteRemovesSourceAndItsChunks() {
        UUID id = UUID.randomUUID();
        IngestedFile file = new IngestedFile(id, "pdf", "/uploads/a.pdf", "docs",
                "hash", "a.pdf", 10, "INGESTED", null, null);
        when(repository.findById(id)).thenReturn(Optional.of(file));

        service.delete(id);

        verify(vectorWriter).deleteBySourceUri("/uploads/a.pdf");
        verify(repository).deleteById(id);
    }

    @Test
    void deleteMissingSourceThrowsSourceNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.SOURCE_NOT_FOUND));

        verify(vectorWriter, never()).deleteBySourceUri(org.mockito.ArgumentMatchers.anyString());
        verify(repository, never()).deleteById(id);
    }
}
