package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request;

import co.edu.uniquindio.triage.domain.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrioritizeRequestRequest(
        @NotNull Priority priority,
        @NotBlank @Size(min = 5, max = 1000) String justification
) {
}
