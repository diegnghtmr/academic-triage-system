package co.edu.uniquindio.triage.application.port.in.auth;

import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.util.Objects;

public record AuthenticatedActor(UserId userId, String username, Role role) {

    public AuthenticatedActor {
        Objects.requireNonNull(userId, "El userId no puede ser null");
        Objects.requireNonNull(role, "El rol no puede ser null");
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El username no puede ser null o vacío");
        }
        username = username.trim();
    }
}
