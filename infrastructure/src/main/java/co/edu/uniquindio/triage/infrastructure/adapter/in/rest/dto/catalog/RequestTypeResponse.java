package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog;

public record RequestTypeResponse(
        Long id,
        String name,
        String description,
        boolean active
) {
}
