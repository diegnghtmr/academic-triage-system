package co.edu.uniquindio.triage.application.port.in.user.command;

import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.util.Objects;
import java.util.Optional;

public record UpdateUserCommand(
        UserId id,
        String firstName,
        String lastName,
        Identification identification,
        Email email,
        Role role,
        boolean active
) {
    public UpdateUserCommand {
        Objects.requireNonNull(id, "El ID de usuario es obligatorio");
        Objects.requireNonNull(firstName, "El nombre es obligatorio");
        Objects.requireNonNull(lastName, "El apellido es obligatorio");
        Objects.requireNonNull(identification, "La identificación es obligatoria");
        Objects.requireNonNull(email, "El email es obligatorio");
        Objects.requireNonNull(role, "El rol es obligatorio");
    }
}
