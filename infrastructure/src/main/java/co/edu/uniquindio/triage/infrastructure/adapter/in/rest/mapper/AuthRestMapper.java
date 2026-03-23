package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper;

import co.edu.uniquindio.triage.application.port.in.auth.AuthResult;
import co.edu.uniquindio.triage.application.port.in.command.LoginCommand;
import co.edu.uniquindio.triage.application.port.in.command.RegisterCommand;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.auth.AuthResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.auth.LoginRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.auth.RegisterRequest;

import java.util.Objects;

public class AuthRestMapper {

    private final UserRestMapper userRestMapper;

    public AuthRestMapper(UserRestMapper userRestMapper) {
        this.userRestMapper = Objects.requireNonNull(userRestMapper);
    }

    public RegisterCommand toCommand(RegisterRequest request) {
        return new RegisterCommand(
                new Username(request.username()),
                new Email(request.email()),
                request.password(),
                request.firstName(),
                request.lastName(),
                new Identification(request.identification()),
                request.role()
        );
    }

    public LoginCommand toCommand(LoginRequest request) {
        return new LoginCommand(new Username(request.username()), request.password());
    }

    public AuthResponse toResponse(AuthResult authResult) {
        return new AuthResponse(
                authResult.authToken().token(),
                authResult.authToken().tokenType(),
                authResult.authToken().expiresIn(),
                userRestMapper.toResponse(authResult.user())
        );
    }
}
