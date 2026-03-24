package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ClassifyRequestRequest(
        @NotNull @Positive Long requestTypeId,
        @Size(max = 1000) String observations
) {
}
