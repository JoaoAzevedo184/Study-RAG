package com.joaoazevedo.studyrag.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.joaoazevedo.studyrag.api.dto.IngestRequestDto;
import com.joaoazevedo.studyrag.api.dto.IngestResponseDto;
import com.joaoazevedo.studyrag.ingestion.IngestCommand;
import com.joaoazevedo.studyrag.ingestion.IngestionResult;
import com.joaoazevedo.studyrag.ingestion.IngestionService;

import jakarta.validation.Valid;

/**
 * POST /ingest (UC-1). Validações de campo (RN-1/RN-4) ocorrem via bean
 * validation no DTO; erros de domínio (arquivo ausente, tipo não extraível) são
 * traduzidos pelo {@link ApiExceptionHandler}.
 */
@RestController
public class IngestionController {

    private static final String DEFAULT_COLLECTION = "default";

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponseDto> ingest(@Valid @RequestBody IngestRequestDto request) {
        String collection = (request.collection() == null || request.collection().isBlank())
                ? DEFAULT_COLLECTION
                : request.collection();

        IngestCommand command = new IngestCommand(
                request.sourceType(), request.sourceUri(), collection, request.metadata());

        IngestionResult result = ingestionService.ingest(command);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(IngestResponseDto.from(result));
    }
}
