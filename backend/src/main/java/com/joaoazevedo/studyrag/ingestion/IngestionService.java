package com.joaoazevedo.studyrag.ingestion;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joaoazevedo.studyrag.ingestion.chunking.RecursiveCharacterChunker;
import com.joaoazevedo.studyrag.ingestion.dedup.ContentHasher;
import com.joaoazevedo.studyrag.ingestion.dedup.IngestedFile;
import com.joaoazevedo.studyrag.ingestion.dedup.IngestedFileRepository;
import com.joaoazevedo.studyrag.ingestion.reader.DocumentExtractorFactory;
import com.joaoazevedo.studyrag.ingestion.reader.ExtractedDocument;
import com.joaoazevedo.studyrag.error.ApiException;

/**
 * Orquestra o pipeline de ingestão (UC-1): resolução do arquivo, extração de
 * texto, hash, deduplicação (RN-2/RN-3), chunking determinístico (RN-5) e
 * gravação dos chunks no vector store com metadados (source_uri, collection,
 * page, title).
 */
@Service
public class IngestionService {

    private static final String STATUS_INGESTED = "INGESTED";

    private final UploadPathResolver pathResolver;
    private final DocumentExtractorFactory extractorFactory;
    private final ContentHasher hasher;
    private final RecursiveCharacterChunker chunker;
    private final VectorChunkWriter vectorWriter;
    private final IngestedFileRepository repository;

    public IngestionService(UploadPathResolver pathResolver,
                            DocumentExtractorFactory extractorFactory,
                            ContentHasher hasher,
                            RecursiveCharacterChunker chunker,
                            VectorChunkWriter vectorWriter,
                            IngestedFileRepository repository) {
        this.pathResolver = pathResolver;
        this.extractorFactory = extractorFactory;
        this.hasher = hasher;
        this.chunker = chunker;
        this.vectorWriter = vectorWriter;
        this.repository = repository;
    }

    @Transactional
    public IngestionResult ingest(IngestCommand command) {
        Path file = pathResolver.resolveExisting(command.sourceUri());          // 404 se ausente

        ExtractedDocument extracted = extractorFactory.extract(command.sourceType(), file); // 415 se falhar
        if (extracted.isEmpty()) {
            throw ApiException.unsupportedType(
                    "Nenhum texto extraível em: " + command.sourceUri());
        }

        String hash = hasher.sha256(extracted.canonicalText());                 // RN-3
        Optional<IngestedFile> existing = repository.findBySourceUri(command.sourceUri()); // RN-2

        // RN-3: mesmo sourceUri + mesmo hash ⇒ pula sem reprocessar.
        if (existing.isPresent() && existing.get().contentHash().equals(hash)) {
            return new IngestionResult(existing.get().id(), IngestStatus.SKIPPED_UNCHANGED, 0);
        }

        String title = resolveTitle(command, file);
        List<Document> chunks = buildChunks(command, extracted, title);

        if (existing.isPresent()) {
            // RN-3: hash mudou ⇒ remove chunks antigos e reindexa (atualização incremental).
            UUID id = existing.get().id();
            vectorWriter.deleteBySourceUri(command.sourceUri());
            vectorWriter.write(chunks);
            repository.update(id, command.collection(), hash, title, chunks.size(), STATUS_INGESTED);
            return new IngestionResult(id, IngestStatus.INGESTED, chunks.size());
        }

        // Fonte nova.
        UUID id = UUID.randomUUID();
        vectorWriter.write(chunks);
        repository.insert(new IngestedFile(
                id, command.sourceType().value(), command.sourceUri(), command.collection(),
                hash, title, chunks.size(), STATUS_INGESTED, null, null));
        return new IngestionResult(id, IngestStatus.INGESTED, chunks.size());
    }

    /** Fragmenta cada segmento (RN-5) e anexa os metadados de cada chunk. */
    private List<Document> buildChunks(IngestCommand command, ExtractedDocument extracted, String title) {
        List<Document> chunks = new ArrayList<>();
        for (ExtractedDocument.Segment segment : extracted.segments()) {
            for (String text : chunker.chunk(segment.text())) {
                chunks.add(new Document(text, baseMetadata(command, title, segment.page())));
            }
        }
        return chunks;
    }

    private Map<String, Object> baseMetadata(IngestCommand command, String title, Integer page) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(ChunkMetadata.SOURCE_URI, command.sourceUri());
        metadata.put(ChunkMetadata.COLLECTION, command.collection());
        if (title != null) {
            metadata.put(ChunkMetadata.TITLE, title);
        }
        if (page != null) {
            metadata.put(ChunkMetadata.PAGE, page);
        }
        if (command.metadata() != null) {
            command.metadata().forEach(metadata::putIfAbsent);
        }
        return metadata;
    }

    private String resolveTitle(IngestCommand command, Path file) {
        if (command.metadata() != null) {
            String title = command.metadata().get("title");
            if (title != null && !title.isBlank()) {
                return title;
            }
        }
        return file.getFileName().toString();
    }
}
