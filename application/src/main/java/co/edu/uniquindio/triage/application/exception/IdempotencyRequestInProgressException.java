package co.edu.uniquindio.triage.application.exception;

public class IdempotencyRequestInProgressException extends RuntimeException {
    public IdempotencyRequestInProgressException() {
        super("Ya existe una solicitud en procesamiento para la misma clave de idempotencia");
    }
}
