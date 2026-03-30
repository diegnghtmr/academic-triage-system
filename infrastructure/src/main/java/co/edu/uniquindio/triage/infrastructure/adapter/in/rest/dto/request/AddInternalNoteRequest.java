package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddInternalNoteRequest(
        @NotBlank(message = "Las observaciones no pueden estar vacías")
        @Size(max = 2000, message = "Las observaciones no pueden tener más de 2000 caracteres")
        String observations
) {
}
