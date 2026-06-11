package com.joaoazevedo.studyrag.ingestion;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Tipos de fonte suportados no MVP (RN-1). A (de)serialização usa o valor
 * minúsculo do contrato ({@code pdf} / {@code markdown}); qualquer outro valor
 * recebido na requisição causa falha de desserialização, mapeada para
 * VALIDATION_ERROR (400).
 */
public enum SourceType {
    PDF("pdf"),
    MARKDOWN("markdown");

    private final String value;

    SourceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static SourceType fromValue(String value) {
        for (SourceType t : values()) {
            if (t.value.equalsIgnoreCase(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("sourceType inválido: " + value);
    }
}
