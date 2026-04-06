package co.edu.uniquindio.triage.application.port.out.security;

import co.edu.uniquindio.triage.domain.model.User;

public interface TokenProviderPort {

    AuthToken issue(User user);

    AuthenticatedUserPayload parse(String token);
}
