package co.edu.uniquindio.triage.infrastructure.idempotency;

import java.util.Objects;
import java.util.Optional;

public record IdempotencyExecutionResult(
        Status status,
        Optional<IdempotencyResponseEnvelope> response
) {
    public enum Status {
        PROCESSED,
        REPLAY,
        MISMATCH,
        OUTSTANDING
    }

    public IdempotencyExecutionResult {
        Objects.requireNonNull(status, "El status no puede ser null");
        response = response == null ? Optional.empty() : response;
    }

    public static IdempotencyExecutionResult processed(IdempotencyResponseEnvelope response) {
        return new IdempotencyExecutionResult(Status.PROCESSED, Optional.of(response));
    }

    public static IdempotencyExecutionResult replay(IdempotencyResponseEnvelope response) {
        return new IdempotencyExecutionResult(Status.REPLAY, Optional.of(response));
    }

    public static IdempotencyExecutionResult mismatch() {
        return new IdempotencyExecutionResult(Status.MISMATCH, Optional.empty());
    }

    public static IdempotencyExecutionResult outstanding() {
        return new IdempotencyExecutionResult(Status.OUTSTANDING, Optional.empty());
    }
}
