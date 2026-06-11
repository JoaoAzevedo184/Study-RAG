package com.joaoazevedo.studyrag.ingestion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.joaoazevedo.studyrag.error.ApiException;
import com.joaoazevedo.studyrag.ingestion.dedup.IngestedFile;
import com.joaoazevedo.studyrag.ingestion.dedup.IngestedFileRepository;

/**
 * Consulta e remoção de fontes (UC-2, UC-3). É também o ponto de acoplamento de
 * leitura para os pacotes {@code retrieval}/{@code generation}: a busca por
 * {@code sourceUri} permite enriquecer citações (título/coleção) sem reabrir o
 * documento.
 */
@Service
public class SourceService {

    private final IngestedFileRepository repository;
    private final VectorChunkWriter vectorWriter;

    public SourceService(IngestedFileRepository repository, VectorChunkWriter vectorWriter) {
        this.repository = repository;
        this.vectorWriter = vectorWriter;
    }

    /** Lista fontes, opcionalmente filtrando por coleção (UC-2). */
    public List<IngestedFile> list(String collection) {
        if (collection == null || collection.isBlank()) {
            return repository.findAll();
        }
        return repository.findByCollection(collection);
    }

    /** Metadados de uma fonte pelo seu {@code sourceUri} (citações em generation). */
    public Optional<IngestedFile> findBySourceUri(String sourceUri) {
        return repository.findBySourceUri(sourceUri);
    }

    /**
     * Remove a fonte e todos os seus chunks no vector store (RN-9, atômico para
     * o usuário).
     *
     * @throws ApiException SOURCE_NOT_FOUND (404) se o {@code fileId} não existir.
     */
    @Transactional
    public void delete(UUID fileId) {
        IngestedFile file = repository.findById(fileId)
                .orElseThrow(() -> ApiException.sourceNotFound(fileId.toString()));
        vectorWriter.deleteBySourceUri(file.sourceUri());
        repository.deleteById(fileId);
    }
}
