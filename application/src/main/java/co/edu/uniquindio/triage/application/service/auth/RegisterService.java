package co.edu.uniquindio.triage.application.service.auth;

import co.edu.uniquindio.triage.application.port.in.auth.RegisterUseCase;
import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.RegisterCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveUserPort;
import co.edu.uniquindio.triage.application.port.out.security.PasswordEncoderPort;
import co.edu.uniquindio.triage.domain.exception.DuplicateUserException;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.User;

import java.util.Objects;
import java.util.Optional;

public class RegisterService implements RegisterUseCase {

    private final LoadUserAuthPort loadUserAuthPort;
    private final SaveUserPort saveUserPort;
    private final PasswordEncoderPort passwordEncoderPort;

    public RegisterService(LoadUserAuthPort loadUserAuthPort,
                           SaveUserPort saveUserPort,
                           PasswordEncoderPort passwordEncoderPort) {
        this.loadUserAuthPort = Objects.requireNonNull(loadUserAuthPort);
        this.saveUserPort = Objects.requireNonNull(saveUserPort);
        this.passwordEncoderPort = Objects.requireNonNull(passwordEncoderPort);
    }

    @Override
    public User register(RegisterCommand command, Optional<AuthenticatedActor> actor) {
        Objects.requireNonNull(command, "El command no puede ser null");
        Objects.requireNonNull(actor, "El actor no puede ser null");

        validateDuplicates(command);

        var resolvedRole = User.resolveRegistrationRole(
                command.requestedRole(),
                actor.map(AuthenticatedActor::role).orElse(null)
        );
        var passwordHash = new PasswordHash(passwordEncoderPort.encode(command.rawPassword()));
        var user = User.registerNew(
                command.username(),
                command.firstName(),
                command.lastName(),
                passwordHash,
                command.identification(),
                command.email(),
                resolvedRole
        );

        return saveUserPort.save(user);
    }

    private void validateDuplicates(RegisterCommand command) {
        if (loadUserAuthPort.existsByUsername(command.username())) {
            throw new DuplicateUserException("username", command.username().value());
        }
        if (loadUserAuthPort.existsByEmail(command.email())) {
            throw new DuplicateUserException("email", command.email().value());
        }
        if (loadUserAuthPort.existsByIdentification(command.identification())) {
            throw new DuplicateUserException("identification", command.identification().value());
        }
    }
}
