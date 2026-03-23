package co.edu.uniquindio.triage.domain.exception;

public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException() {
        super("Credenciales inválidas o usuario inactivo");
    }
}
