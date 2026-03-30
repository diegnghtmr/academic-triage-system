package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user;

import co.edu.uniquindio.triage.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 75, message = "El nombre no puede exceder 75 caracteres")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 75, message = "El apellido no puede exceder 75 caracteres")
        String lastName,

        @NotBlank(message = "La identificación es obligatoria")
        @Size(max = 20, message = "La identificación no puede exceder 20 caracteres")
        String identification,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email es inválido")
        String email,

        @NotNull(message = "El rol es obligatorio")
        Role role,

        @NotNull(message = "El estado activo es obligatorio")
        Boolean active
) {
}
