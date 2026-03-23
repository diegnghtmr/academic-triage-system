package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;

@FunctionalInterface
public interface AuthenticatedRequestUseCase<C, R> {

    /**
     * Future request flows start with the minimal actor payload and only rehydrate the canonical
     * user through persistence ports when mutable user state is required by business invariants.
     */
    R execute(C command, AuthenticatedActor actor);
}
