package com.joaoazevedo.studyrag.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.joaoazevedo.studyrag.error.ApiException;
import com.joaoazevedo.studyrag.error.ErrorCode;
import com.joaoazevedo.studyrag.ingestion.chunking.RecursiveCharacterChunker;
import com.joaoazevedo.studyrag.ingestion.dedup.ContentHasher;
import com.joaoazevedo.studyrag.ingestion.dedup.IngestedFile;
import com.joaoazevedo.studyrag.ingestion.dedup.IngestedFileRepository;
import com.joaoazevedo.studyrag.ingestion.reader.DocumentExtractorFactory;
import com.joaoazevedo.studyrag.ingestion.reader.ExtractedDocument;
import com.joaoazevedo.studyrag.ingestion.reader.ExtractedDocument.Segment;

/** Cobre a deduplicação por hash (RN-2/RN-3) e os erros de ingestão. */
class IngestionServiceTest {

    private static final String URI = "/uploads/guia.pdf";
    private static final String TEXT = "Conteúdo extraído do guia sobre RAG e chunking recursivo.";

    private UploadPathResolver pathResolver;
    private DocumentExtractorFactory extractorFactory;
    private VectorChunkWriter vectorWriter;
    private IngestedFileRepository repository;

    private final ContentHasher hasher = new ContentHasher();
    private IngestionService service;

    @BeforeEach
    void setUp() {
        pathResolver = mock(UploadPathResolver.class);
        extractorFactory = mock(DocumentExtractorFactory.class);
        vectorWriter = mock(VectorChunkWriter.class);
        repository = mock(IngestedFileRepository.class);

        service = new IngestionService(
                pathResolver, extractorFactory, hasher,
                new RecursiveCharacterChunker(1000, 200), vectorWriter, repository);
    }

    private IngestCommand command() {
        return new IngestCommand(SourceType.PDF, URI, "docs", Map.of());
    }

    private void stubExtraction(String text) {
        Path path = Paths.get(URI);
        when(pathResolver.resolveExisting(URI)).thenReturn(path);
        when(extractorFactory.extract(eq(SourceType.PDF), eq(path)))
                .thenReturn(new ExtractedDocument(List.of(new Segment(text, 1))));
    }

    @Test
    void newSourceIsIngested() {
        stubExtraction(TEXT);
        when(repository.findBySourceUri(URI)).thenReturn(Optional.empty());

        IngestionResult result = service.ingest(command());

        assertThat(result.status()).isEqualTo(IngestStatus.INGESTED);
        assertThat(result.chunkCount()).isGreaterThan(0);
        assertThat(result.fileId()).isNotNull();
        verify(vectorWriter).write(anyList());
        verify(repository).insert(any(IngestedFile.class));
        verify(vectorWriter, never()).deleteBySourceUri(anyString());
        verify(repository, never()).update(any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void sameHashIsSkippedUnchanged() {
        stubExtraction(TEXT);
        UUID existingId = UUID.randomUUID();
        String sameHash = hasher.sha256(TEXT);
        when(repository.findBySourceUri(URI)).thenReturn(Optional.of(existing(existingId, sameHash)));

        IngestionResult result = service.ingest(command());

        assertThat(result.status()).isEqualTo(IngestStatus.SKIPPED_UNCHANGED);
        assertThat(result.chunkCount()).isZero();
        assertThat(result.fileId()).isEqualTo(existingId);
        verify(vectorWriter, never()).write(anyList());
        verify(vectorWriter, never()).deleteBySourceUri(anyString());
        verify(repository, never()).insert(any());
        verify(repository, never()).update(any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void changedHashTriggersReindex() {
        stubExtraction(TEXT);
        UUID existingId = UUID.randomUUID();
        when(repository.findBySourceUri(URI)).thenReturn(Optional.of(existing(existingId, "hash-antigo-diferente")));

        IngestionResult result = service.ingest(command());

        assertThat(result.status()).isEqualTo(IngestStatus.INGESTED);
        assertThat(result.fileId()).isEqualTo(existingId);
        verify(vectorWriter).deleteBySourceUri(URI);
        verify(vectorWriter).write(anyList());
        verify(repository).update(eq(existingId), eq("docs"), eq(hasher.sha256(TEXT)),
                anyString(), anyInt(), eq("INGESTED"));
        verify(repository, never()).insert(any());
    }

    @Test
    void emptyExtractionIsUnsupportedType() {
        Path path = Paths.get(URI);
        when(pathResolver.resolveExisting(URI)).thenReturn(path);
        when(extractorFactory.extract(eq(SourceType.PDF), eq(path)))
                .thenReturn(new ExtractedDocument(List.of(new Segment("   ", null))));

        assertThatThrownBy(() -> service.ingest(command()))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.UNSUPPORTED_TYPE));
    }

    @Test
    void missingFilePropagatesSourceFileNotFound() {
        when(pathResolver.resolveExisting(URI)).thenThrow(ApiException.sourceFileNotFound(URI));

        assertThatThrownBy(() -> service.ingest(command()))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.SOURCE_FILE_NOT_FOUND));
    }

    private IngestedFile existing(UUID id, String hash) {
        return new IngestedFile(id, "pdf", URI, "docs", hash, "guia.pdf", 40, "INGESTED", null, null);
    }
}
