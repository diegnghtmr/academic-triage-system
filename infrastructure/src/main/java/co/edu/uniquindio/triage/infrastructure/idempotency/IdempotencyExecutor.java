package co.edu.uniquindio.triage.infrastructure.idempotency;

import java.util.function.Supplier;

public interface IdempotencyExecutor {
    IdempotencyExecutionResult execute(IdempotencyRequest request, Supplier<IdempotencyResponseEnvelope> processor);
}
