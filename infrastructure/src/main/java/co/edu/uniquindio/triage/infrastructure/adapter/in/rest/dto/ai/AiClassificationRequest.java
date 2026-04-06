package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record AiClassificationRequest(
    @NotBlank(message = "La descripción no puede estar vacía")
    String description
) {
}
