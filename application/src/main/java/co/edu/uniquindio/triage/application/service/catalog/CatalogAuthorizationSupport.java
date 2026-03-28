package co.edu.uniquindio.triage.application.service.catalog;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;

import java.util.Objects;

final class CatalogAuthorizationSupport {

    private CatalogAuthorizationSupport() {
    }

    static void ensureAuthenticatedActor(AuthenticatedActor actor) {
        Objects.requireNonNull(actor, "El actor no puede ser null");
    }

    static void ensureAdminActor(AuthenticatedActor actor, String operation) {
        ensureAuthenticatedActor(actor);
        if (actor.role() != Role.ADMIN) {
            throw new UnauthorizedOperationException(actor.role(), operation);
        }
    }
}
