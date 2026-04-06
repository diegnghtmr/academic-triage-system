package co.edu.uniquindio.triage.application.service.report;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;

import java.util.Objects;

public class ReportAuthorizationSupport {

    public void ensureAdmin(AuthenticatedActor actor) {
        Objects.requireNonNull(actor, "El actor no puede ser null");
        if (actor.role() != Role.ADMIN) {
            throw new UnauthorizedOperationException(
                Role.ADMIN,
                "Solo los administradores pueden acceder a las métricas del dashboard"
            );
        }
    }
}
