package co.edu.uniquindio.triage.application.port.out.security;

import co.edu.uniquindio.triage.domain.enums.Role;

import java.util.Objects;

public record AuthenticatedUserPayload(Long userId, String username, Role role) {

    public AuthenticatedUserPayload {
        Objects.requireNonNull(userId, "El userId no puede ser null");
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El username no puede ser null o vacío");
        }
        Objects.requireNonNull(role, "El rol no puede ser null");
        username = username.trim();
    }
}
