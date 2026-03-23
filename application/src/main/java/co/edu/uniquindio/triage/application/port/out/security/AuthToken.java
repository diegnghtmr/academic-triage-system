package co.edu.uniquindio.triage.application.port.out.security;

import java.util.Objects;

public record AuthToken(String token, String tokenType, long expiresIn) {

    public AuthToken {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token no puede ser null o vacío");
        }
        if (tokenType == null || tokenType.isBlank()) {
            throw new IllegalArgumentException("El tipo de token no puede ser null o vacío");
        }
        if (expiresIn <= 0) {
            throw new IllegalArgumentException("La expiración debe ser positiva");
        }

        token = token.trim();
        tokenType = tokenType.trim();
        Objects.requireNonNull(token);
    }
}
