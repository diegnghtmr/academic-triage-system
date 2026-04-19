package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request DTO.
 * <p>
 * {@code identifier} is the canonical field (accepts username or email).
 * {@code username} is a deprecated alias kept for backward compatibility during the transition window.
 * At most one must be provided; if both are present, they must be logically equivalent (after trim).
 * Providing both with different values is a 400 contract violation.
 * </p>
 */
public record LoginRequest(
        @Size(min = 3, max = 255) String identifier,
        @Deprecated @Size(min = 3, max = 255) String username,
        @NotBlank @Size(min = 8) String password
) {

    public LoginRequest {
        var hasIdentifier = identifier != null && !identifier.isBlank();
        var hasUsername = username != null && !username.isBlank();

        if (!hasIdentifier && !hasUsername) {
            throw new IllegalArgumentException("Se requiere al menos 'identifier' o 'username' para autenticarse");
        }

        if (hasIdentifier && hasUsername) {
            var canonicalTrimmed = identifier.trim();
            var aliasTrimmed = username.trim();
            if (!canonicalTrimmed.equalsIgnoreCase(aliasTrimmed)) {
                throw new IllegalArgumentException(
                        "Los campos 'identifier' y 'username' tienen valores distintos; proporcione solo uno");
            }
        }
    }

    /** Returns the effective canonical identifier, preferring {@code identifier} over the alias. */
    public String effectiveIdentifier() {
        if (identifier != null && !identifier.isBlank()) {
            return identifier.trim();
        }
        return username.trim();
    }
}
