package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AssignRequestRequest(
        @NotNull @Positive Long assignedToUserId,
        @Size(max = 1000) String observations
) {
}
