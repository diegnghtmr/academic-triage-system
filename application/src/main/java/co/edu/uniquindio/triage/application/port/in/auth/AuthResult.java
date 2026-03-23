package co.edu.uniquindio.triage.application.port.in.auth;

import co.edu.uniquindio.triage.application.port.out.security.AuthToken;
import co.edu.uniquindio.triage.domain.model.User;

import java.util.Objects;

public record AuthResult(AuthToken authToken, User user) {

    public AuthResult {
        Objects.requireNonNull(authToken, "El token no puede ser null");
        Objects.requireNonNull(user, "El usuario no puede ser null");
    }
}
