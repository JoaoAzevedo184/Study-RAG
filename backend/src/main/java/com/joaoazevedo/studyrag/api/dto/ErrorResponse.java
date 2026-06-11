package com.joaoazevedo.studyrag.api.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Corpo de erro (schema {@code Error}): {@code code} estável em maiúsculas e
 * {@code message} legível (em português). {@code details} é omitido quando nulo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, Map<String, Object> details) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }
}
