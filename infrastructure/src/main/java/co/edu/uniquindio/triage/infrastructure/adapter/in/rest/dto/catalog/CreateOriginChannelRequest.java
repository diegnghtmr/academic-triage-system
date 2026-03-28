package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOriginChannelRequest(@NotBlank @Size(max = 100) String name) {
}
