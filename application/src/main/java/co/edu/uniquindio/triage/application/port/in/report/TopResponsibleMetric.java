package co.edu.uniquindio.triage.application.port.in.report;

import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.id.UserId;

public record TopResponsibleMetric(
    UserId userId,
    String username,
    String firstName,
    String lastName,
    String identification,
    String email,
    Role role,
    boolean active,
    long closedRequestsCount
) {
}
