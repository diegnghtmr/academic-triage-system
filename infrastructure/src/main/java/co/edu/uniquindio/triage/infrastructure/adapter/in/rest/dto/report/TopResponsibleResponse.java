package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.report;

import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user.UserResponse;

public record TopResponsibleResponse(
    UserResponse user,
    long resolvedCount
) {
}
