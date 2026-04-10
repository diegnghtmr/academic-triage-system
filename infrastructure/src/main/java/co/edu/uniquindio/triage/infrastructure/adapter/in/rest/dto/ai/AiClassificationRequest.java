package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiClassificationRequest(
    @NotBlank(message = "La descripción no puede estar vacía")
    @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2000 caracteres")
    String description
) {
}
