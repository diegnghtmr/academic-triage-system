package co.edu.uniquindio.triage.application.exception;

public class ETagMismatchException extends RuntimeException {
    public ETagMismatchException() {
        super("El ETag enviado en If-Match no coincide con la versión actual del recurso");
    }
}
