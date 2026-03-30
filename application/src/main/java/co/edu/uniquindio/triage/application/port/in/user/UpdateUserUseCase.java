package co.edu.uniquindio.triage.application.port.in.user;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.user.command.UpdateUserCommand;
import co.edu.uniquindio.triage.domain.model.User;

public interface UpdateUserUseCase {
    User execute(UpdateUserCommand command, AuthenticatedActor actor);
}
