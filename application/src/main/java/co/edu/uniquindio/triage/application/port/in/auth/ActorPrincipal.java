package co.edu.uniquindio.triage.application.port.in.auth;

import co.edu.uniquindio.triage.domain.enums.Role;

public interface ActorPrincipal {
    Long id();
    String username();
    Role role();
}
