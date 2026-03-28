package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelRequestRequest(
        @NotBlank @Size(min = 5, max = 2000) String cancellationReason
) {
}
