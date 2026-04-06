package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support;

import co.edu.uniquindio.triage.application.port.in.auth.ActorPrincipal;
import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthenticatedActorMapper {

    public Optional<AuthenticatedActor> toOptionalActor(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof ActorPrincipal actorPrincipal)) {
            return Optional.empty();
        }

        return Optional.of(new AuthenticatedActor(
                UserId.of(actorPrincipal.id()),
                actorPrincipal.username(),
                actorPrincipal.role()
        ));
    }

    public AuthenticatedActor toRequiredActor(Authentication authentication) {
        return toOptionalActor(authentication)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("No hay actor autenticado en el contexto actual"));
    }
}
