package co.edu.uniquindio.triage.application.exception;

public class MissingIfMatchPreconditionException extends RuntimeException {
    public MissingIfMatchPreconditionException() {
        super("El header If-Match es obligatorio para esta operación");
    }
}
