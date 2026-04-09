package co.edu.uniquindio.triage.application.port.in.user;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.util.Optional;

public interface GetUserByIdQuery {
    Optional<User> execute(UserId id, AuthenticatedActor actor);
}
