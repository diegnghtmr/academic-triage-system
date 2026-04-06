package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.auth;

import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user.UserResponse;

public record AuthResponse(String token, String tokenType, long expiresIn, UserResponse user) {
}
