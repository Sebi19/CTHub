package org.cthub.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.cthub.backend.dto.auth.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Translates application exceptions into the shared {@link ErrorResponseDto} shape so every
 * endpoint reports failures identically and the frontend can branch on {@code code}.
 */
@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponseDto> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponseDto.builder()
            .code(ex.getCode())
            .message(ex.getMessage())
            .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponseDto.builder()
            .code("VALIDATION_FAILED")
            .message(message.isBlank() ? "Request validation failed." : message)
            .build());
    }
}
