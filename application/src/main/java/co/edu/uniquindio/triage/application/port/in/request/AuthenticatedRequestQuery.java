package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;

@FunctionalInterface
public interface AuthenticatedRequestQuery<Q, R> {

    /**
     * Query-side request flows receive the same minimal actor seam; caller rehydration remains an
     * explicit opt-in for cases where persisted user state matters.
     */
    R execute(Q query, AuthenticatedActor actor);
}
