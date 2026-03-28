package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRequestTypeRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description
) {
}
