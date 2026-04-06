package co.edu.uniquindio.triage.infrastructure.adapter.out.security;

import co.edu.uniquindio.triage.application.port.out.security.AuthToken;
import co.edu.uniquindio.triage.application.port.out.security.AuthenticatedUserPayload;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public class JwtTokenAdapter implements TokenProviderPort {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenAdapter(String secret, long expirationMs) {
        Objects.requireNonNull(secret, "El secret JWT no puede ser null");
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    @Override
    public AuthToken issue(User user) {
        if (user.getId().isEmpty()) {
            throw new IllegalArgumentException("No se puede emitir JWT para un usuario sin id persistido");
        }

        var issuedAt = Instant.now();
        var expiresAt = issuedAt.plusMillis(expirationMs);
        var token = Jwts.builder()
                .subject(user.getUsername().value())
                .claim("uid", user.getId().orElseThrow().value())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();

        return new AuthToken(token, "Bearer", Duration.ofMillis(expirationMs).toSeconds());
    }

    @Override
    public AuthenticatedUserPayload parse(String token) {
        var claims = parseClaims(token);
        var userId = claims.get("uid", Number.class).longValue();
        var role = Role.valueOf(claims.get("role", String.class));
        return new AuthenticatedUserPayload(userId, claims.getSubject(), role);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
