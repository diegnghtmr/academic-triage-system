package co.edu.uniquindio.triage.application.service.ai;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;

import java.util.Objects;

public class AiAuthorizationSupport {

    public void ensureStaff(AuthenticatedActor actor) {
        Objects.requireNonNull(actor, "El actor no puede ser null");
        if (actor.role() != Role.STAFF) {
            throw new UnauthorizedOperationException(Role.STAFF, "Solo el personal administrativo puede solicitar sugerencias de IA");
        }
    }

    public void ensureStaffOrAdmin(AuthenticatedActor actor) {
        Objects.requireNonNull(actor, "El actor no puede ser null");
        if (actor.role() != Role.STAFF && actor.role() != Role.ADMIN) {
            throw new UnauthorizedOperationException(Role.STAFF, "Solo el personal administrativo o administradores pueden generar resúmenes de IA");
        }
    }
}
