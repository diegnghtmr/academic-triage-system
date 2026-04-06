package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.auth;

import co.edu.uniquindio.triage.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank @Size(max = 75) String firstName,
        @NotBlank @Size(max = 75) String lastName,
        @NotBlank @Size(max = 20) String identification,
        Role role
) {
}
