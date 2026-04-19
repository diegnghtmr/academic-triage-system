package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice;

import co.edu.uniquindio.triage.application.exception.ETagMismatchException;
import co.edu.uniquindio.triage.application.exception.IdempotencyFingerprintMismatchException;
import co.edu.uniquindio.triage.application.exception.IdempotencyRequestInProgressException;
import co.edu.uniquindio.triage.application.exception.MissingIdempotencyKeyException;
import co.edu.uniquindio.triage.application.exception.MissingIfMatchPreconditionException;
import co.edu.uniquindio.triage.domain.exception.AiServiceUnavailableException;
import co.edu.uniquindio.triage.domain.exception.AmbiguousLoginIdentifierException;
import co.edu.uniquindio.triage.domain.exception.AuthenticationFailedException;
import co.edu.uniquindio.triage.domain.exception.BusinessRuleViolationException;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.exception.DuplicateUserException;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.exception.InvalidStateTransitionException;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.common.FieldErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({DuplicateUserException.class, DuplicateCatalogEntryException.class})
    ResponseEntity<ProblemDetail> handleDuplicate(RuntimeException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    @ExceptionHandler(AmbiguousLoginIdentifierException.class)
    ResponseEntity<ProblemDetail> handleAmbiguousLoginIdentifier(AmbiguousLoginIdentifierException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ProblemDetail> handleAuthenticationFailed(AuthenticationFailedException exception) {
        return build(HttpStatus.UNAUTHORIZED, exception.getMessage(), null);
    }

    @ExceptionHandler(UnauthorizedOperationException.class)
    ResponseEntity<ProblemDetail> handleUnauthorizedOperation(UnauthorizedOperationException exception) {
        return build(HttpStatus.FORBIDDEN, exception.getMessage(), null);
    }

    @ExceptionHandler(RequestNotFoundException.class)
    ResponseEntity<ProblemDetail> handleRequestNotFound(RequestNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    ResponseEntity<ProblemDetail> handleAiUnavailable(AiServiceUnavailableException exception) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), null);
    }

    @ExceptionHandler(MissingIdempotencyKeyException.class)
    ResponseEntity<ProblemDetail> handleMissingIdempotencyKey(MissingIdempotencyKeyException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), null, URI.create("urn:problem:idempotency:key-required"));
    }

    @ExceptionHandler(IdempotencyRequestInProgressException.class)
    ResponseEntity<ProblemDetail> handleIdempotencyInProgress(IdempotencyRequestInProgressException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), null, URI.create("urn:problem:idempotency:request-in-progress"));
    }

    @ExceptionHandler(IdempotencyFingerprintMismatchException.class)
    ResponseEntity<ProblemDetail> handleIdempotencyMismatch(IdempotencyFingerprintMismatchException exception) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), null, URI.create("urn:problem:idempotency:fingerprint-mismatch"));
    }

    @ExceptionHandler(MissingIfMatchPreconditionException.class)
    ResponseEntity<ProblemDetail> handleMissingIfMatch(MissingIfMatchPreconditionException exception) {
        return build(HttpStatus.PRECONDITION_REQUIRED, exception.getMessage(), null, URI.create("urn:problem:precondition:if-match-required"));
    }

    @ExceptionHandler(ETagMismatchException.class)
    ResponseEntity<ProblemDetail> handleEtagMismatch(ETagMismatchException exception) {
        return build(HttpStatus.PRECONDITION_FAILED, exception.getMessage(), null, URI.create("urn:problem:precondition:etag-mismatch"));
    }

    @ExceptionHandler({InvalidStateTransitionException.class, BusinessRuleViolationException.class, IllegalStateException.class})
    ResponseEntity<ProblemDetail> handleConflict(RuntimeException exception) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ProblemDetail> handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException exception) {
        return build(HttpStatus.CONFLICT,
                "El recurso fue modificado por otra operación concurrente. Recargá el estado y reintentá",
                null,
                URI.create("urn:problem:concurrency:optimistic-lock"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        var fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "La solicitud contiene errores de validación", fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return build(HttpStatus.BAD_REQUEST,
                "El parámetro '%s' tiene un valor inválido".formatted(exception.getName()),
                null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException exception) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleUnhandledDataIntegrity(DataIntegrityViolationException exception) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "La operación no pudo completarse por una restricción de base de datos",
                null);
    }

    private FieldErrorResponse toFieldError(FieldError fieldError) {
        return new FieldErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String detail, List<FieldErrorResponse> fieldErrors) {
        return build(status, detail, fieldErrors, null);
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status,
                                                String detail,
                                                List<FieldErrorResponse> fieldErrors,
                                                URI type) {
        var problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        if (type != null) {
            problemDetail.setType(type);
        }

        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            problemDetail.setProperty("fieldErrors", fieldErrors);
        }

        return ResponseEntity.status(status).body(problemDetail);
    }
}
