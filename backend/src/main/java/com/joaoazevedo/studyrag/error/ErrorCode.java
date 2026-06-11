package com.joaoazevedo.studyrag.error;

import org.springframework.http.HttpStatus;

/**
 * Códigos de erro estáveis do MVP (Seção 7 do SDD-MVP). O {@code code} é a string
 * em maiúsculas que o frontend usa em lógica de tratamento; o status HTTP associado
 * é o contratado no OpenAPI.
 */
public enum ErrorCode {

    /** Validação de campos falhou. */
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),

    /** Arquivo não encontrado no diretório de uploads (ingestão). */
    SOURCE_FILE_NOT_FOUND(HttpStatus.NOT_FOUND),

    /** Fonte não encontrada (remoção). */
    SOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),

    /** Tipo de arquivo não suportado no MVP. */
    UNSUPPORTED_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE),

    /** Coleção informada não existe ou está vazia (query). */
    COLLECTION_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
