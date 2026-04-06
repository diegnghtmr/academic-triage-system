package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user;

public record UserResponse(
        Long id,
        String username,
        String firstName,
        String lastName,
        String identification,
        String email,
        String role,
        boolean active
) {
}
