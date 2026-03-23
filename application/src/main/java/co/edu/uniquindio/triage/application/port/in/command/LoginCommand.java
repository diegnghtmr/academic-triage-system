package co.edu.uniquindio.triage.application.port.in.command;

import co.edu.uniquindio.triage.domain.model.Username;

import java.util.Objects;

public record LoginCommand(Username username, String rawPassword) {

    private static final int MIN_PASSWORD_LENGTH = 8;

    public LoginCommand {
        Objects.requireNonNull(username, "El username no puede ser null");
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede ser null o vacía");
        }
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
    }
}
