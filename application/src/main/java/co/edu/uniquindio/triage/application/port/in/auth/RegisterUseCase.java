package co.edu.uniquindio.triage.application.port.in.auth;

import co.edu.uniquindio.triage.application.port.in.command.RegisterCommand;
import co.edu.uniquindio.triage.domain.model.User;

import java.util.Optional;

public interface RegisterUseCase {

    User register(RegisterCommand command, Optional<AuthenticatedActor> actor);
}
