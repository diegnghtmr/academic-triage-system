package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice;

import co.edu.uniquindio.triage.domain.exception.AuthenticationFailedException;
import co.edu.uniquindio.triage.domain.exception.DuplicateUserException;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.common.ErrorResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.common.FieldErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateUserException.class)
    ResponseEntity<ErrorResponse> handleDuplicate(DuplicateUserException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ErrorResponse> handleAuthenticationFailed(AuthenticationFailedException exception) {
        return build(HttpStatus.UNAUTHORIZED, exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        var fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "La solicitud contiene errores de validación", fieldErrors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
    }

    private FieldErrorResponse toFieldError(FieldError fieldError) {
        return new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, List<FieldErrorResponse> fieldErrors) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                OffsetDateTime.now(),
                fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors
        ));
    }
}
