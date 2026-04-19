package co.edu.uniquindio.triage.domain.exception;

/**
 * Raised when a provided login identifier resolves to more than one user record,
 * indicating a data-quality inconsistency. Authentication MUST NOT proceed.
 */
public class AmbiguousLoginIdentifierException extends DomainException {

    public AmbiguousLoginIdentifierException() {
        super("El identificador proporcionado es ambiguo y no permite autenticación segura");
    }
}
