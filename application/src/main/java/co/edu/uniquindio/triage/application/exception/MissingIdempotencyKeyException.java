package co.edu.uniquindio.triage.application.exception;

public class MissingIdempotencyKeyException extends RuntimeException {
    public MissingIdempotencyKeyException() {
        super("El header Idempotency-Key es obligatorio para esta operación");
    }
}
