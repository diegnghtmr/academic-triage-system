package co.edu.uniquindio.triage.application.exception;

public class IdempotencyFingerprintMismatchException extends RuntimeException {
    public IdempotencyFingerprintMismatchException() {
        super("La clave de idempotencia ya fue usada con una solicitud distinta");
    }
}
