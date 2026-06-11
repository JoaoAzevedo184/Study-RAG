package com.joaoazevedo.studyrag.ingestion;

import java.util.List;

import org.springframework.stereotype.Service;

import com.joaoazevedo.studyrag.ingestion.dedup.IngestedFileRepository;
import com.joaoazevedo.studyrag.ingestion.dedup.IngestedFileRepository.CollectionAggregate;

/**
 * Agrega coleções a partir das fontes (UC-4). Coleção não é tabela própria no
 * MVP: deriva do campo {@code collection} das fontes.
 *
 * <p>{@link #exists(String)} é o ponto de acoplamento para o pacote
 * {@code retrieval}: a query deve retornar 422 (COLLECTION_NOT_FOUND) quando a
 * coleção informada não existe (RN-8).</p>
 */
@Service
public class CollectionService {

    private final IngestedFileRepository repository;

    public CollectionService(IngestedFileRepository repository) {
        this.repository = repository;
    }

    /** Coleções existentes com contagem de fontes e de chunks. */
    public List<CollectionAggregate> list() {
        return repository.aggregateCollections();
    }

    /** Há ao menos uma fonte na coleção informada? */
    public boolean exists(String collection) {
        return repository.existsByCollection(collection);
    }
}
