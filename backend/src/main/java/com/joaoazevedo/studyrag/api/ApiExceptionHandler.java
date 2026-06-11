package com.joaoazevedo.studyrag.api;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.joaoazevedo.studyrag.api.dto.ErrorResponse;
import com.joaoazevedo.studyrag.error.ApiException;
import com.joaoazevedo.studyrag.error.ErrorCode;

import jakarta.validation.ConstraintViolationException;

/**
 * Traduz exceções para o schema {@code Error} do OpenAPI com os códigos estáveis
 * da Seção 7 do SDD-MVP. Falhas de (de)serialização e de bean validation viram
 * VALIDATION_ERROR (400).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Erros de domínio com código explícito (404, 415, 422, 400 de regra). */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        ErrorResponse body = new ErrorResponse(ex.getCode().name(), ex.getMessage(), ex.getDetails());
        return ResponseEntity.status(ex.getCode().status()).body(body);
    }

    /** Bean validation no corpo da requisição (ex.: slug de coleção inválido — RN-4). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return validationError(message.isBlank() ? "Requisição inválida" : message);
    }

    /** JSON malformado ou valor de enum inválido (ex.: sourceType "docx" — RN-1). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return validationError("Corpo da requisição inválido ou com valor não suportado");
    }

    /** Parâmetro de tipo inválido (ex.: fileId que não é UUID). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return validationError("Parâmetro inválido: " + ex.getName());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return validationError(ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> validationError(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR.name(), message));
    }
}
