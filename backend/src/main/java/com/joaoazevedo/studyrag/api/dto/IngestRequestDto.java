package com.joaoazevedo.studyrag.api.dto;

import java.util.Map;

import com.joaoazevedo.studyrag.ingestion.SourceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo de POST /ingest (schema {@code IngestRequest}). Espelha o contrato
 * OpenAPI: {@code sourceType} e {@code sourceUri} obrigatórios; {@code collection}
 * é um slug opcional (RN-4) com default {@code default} aplicado no controller.
 */
public record IngestRequestDto(

        @NotNull(message = "sourceType é obrigatório")
        SourceType sourceType,

        @NotBlank(message = "sourceUri é obrigatório")
        String sourceUri,

        // null é válido (default aplicado depois); se presente, deve casar o slug (RN-4).
        @Pattern(regexp = "^[a-z0-9-]+$", message = "collection deve ser um slug minúsculo (a-z, 0-9, hífen)")
        @Size(max = 80, message = "collection deve ter no máximo 80 caracteres")
        String collection,

        Map<String, String> metadata) {
}
