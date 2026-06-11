package com.joaoazevedo.studyrag.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.joaoazevedo.studyrag.api.dto.CollectionDto;
import com.joaoazevedo.studyrag.ingestion.CollectionService;

/** GET /collections (UC-4). */
@RestController
public class CollectionsController {

    private final CollectionService collectionService;

    public CollectionsController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping("/collections")
    public List<CollectionDto> list() {
        return collectionService.list().stream()
                .map(CollectionDto::from)
                .toList();
    }
}
