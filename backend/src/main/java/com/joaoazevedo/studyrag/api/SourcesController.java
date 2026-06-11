package com.joaoazevedo.studyrag.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.joaoazevedo.studyrag.api.dto.SourceDto;
import com.joaoazevedo.studyrag.ingestion.SourceService;

/** GET /sources (UC-2) e DELETE /sources/{fileId} (UC-3, RN-9). */
@RestController
public class SourcesController {

    private final SourceService sourceService;

    public SourcesController(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @GetMapping("/sources")
    public List<SourceDto> list(@RequestParam(required = false) String collection) {
        return sourceService.list(collection).stream()
                .map(SourceDto::from)
                .toList();
    }

    @DeleteMapping("/sources/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable UUID fileId) {
        sourceService.delete(fileId);   // 404 SOURCE_NOT_FOUND se ausente
        return ResponseEntity.noContent().build();
    }
}
