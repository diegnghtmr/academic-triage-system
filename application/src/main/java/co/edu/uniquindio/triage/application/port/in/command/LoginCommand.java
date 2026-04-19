package co.edu.uniquindio.triage.application.port.in.command;

import java.util.Objects;

/**
 * Command that carries a canonical identifier (username or email) for the login use-case.
 * <p>
 * {@code identifier} is the normalized value resolved from the request ({@code identifier} field
 * takes precedence over the deprecated {@code username} alias).
 * {@code isAlias} signals that the value was supplied via the deprecated alias path so the
 * service can emit an observability warn-log without coupling the mapper to the service.
 * </p>
 */
public record LoginCommand(String identifier, boolean isAlias, String rawPassword) {

    private static final int MIN_PASSWORD_LENGTH = 8;

    public LoginCommand {
        Objects.requireNonNull(identifier, "El identifier no puede ser null");
        if (identifier.isBlank()) {
            throw new IllegalArgumentException("El identifier no puede estar vacío");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede ser null o vacía");
        }
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
    }

    /** Convenience constructor for the canonical path (not alias). */
    public LoginCommand(String identifier, String rawPassword) {
        this(identifier, false, rawPassword);
    }
}
