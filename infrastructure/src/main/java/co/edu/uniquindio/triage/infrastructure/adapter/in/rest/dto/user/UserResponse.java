package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user;

import co.edu.uniquindio.triage.domain.enums.Role;

public record UserResponse(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String identification,
        Role role,
        boolean active
) {
}
