package co.edu.uniquindio.triage.application.port.in.command;

import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.Username;

import java.util.Objects;

public record RegisterCommand(
        Username username,
        Email email,
        String rawPassword,
        String fullName,
        Identification identification,
        Role requestedRole
) {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_FULL_NAME_LENGTH = 150;

    public RegisterCommand {
        Objects.requireNonNull(username, "El username no puede ser null");
        Objects.requireNonNull(email, "El email no puede ser null");
        Objects.requireNonNull(identification, "La identificación no puede ser null");

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede ser null o vacía");
        }
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("El nombre completo no puede ser null o vacío");
        }

        fullName = fullName.trim();
        if (fullName.length() > MAX_FULL_NAME_LENGTH) {
            throw new IllegalArgumentException("El nombre completo no puede tener más de 150 caracteres");
        }
    }
}
