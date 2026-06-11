package com.joaoazevedo.studyrag.error;

import java.util.Map;

/**
 * Exceção de domínio que carrega um {@link ErrorCode} estável e, opcionalmente,
 * detalhes. É traduzida pelo handler global no schema {@code Error} do OpenAPI.
 * Pacotes {@code retrieval}/{@code generation} podem lançá-la (ex.:
 * {@link ErrorCode#COLLECTION_NOT_FOUND}) e obter o mapeamento HTTP correto.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final transient Map<String, Object> details;

    public ApiException(ErrorCode code, String message) {
        this(code, message, null);
    }

    public ApiException(ErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public ErrorCode getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    // --- Fábricas para os casos do escopo de ingestão/sources -------------

    public static ApiException sourceFileNotFound(String sourceUri) {
        return new ApiException(ErrorCode.SOURCE_FILE_NOT_FOUND,
                "Arquivo não encontrado no diretório de uploads: " + sourceUri);
    }

    public static ApiException unsupportedType(String detail) {
        return new ApiException(ErrorCode.UNSUPPORTED_TYPE, detail);
    }

    public static ApiException sourceNotFound(String fileId) {
        return new ApiException(ErrorCode.SOURCE_NOT_FOUND,
                "Fonte não encontrada: " + fileId);
    }

    public static ApiException validation(String detail) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, detail);
    }
}
