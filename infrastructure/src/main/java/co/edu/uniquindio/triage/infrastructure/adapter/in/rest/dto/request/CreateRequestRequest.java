package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateRequestRequest(
        @NotNull @Positive Long requestTypeId,
        @NotNull @Positive Long originChannelId,
        @NotBlank @Size(min = 10, max = 2000) String description,
        LocalDate deadline
) {
}
