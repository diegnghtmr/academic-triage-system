package co.edu.uniquindio.triage.application.service.user;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.user.GetUserByIdQuery;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.util.Objects;
import java.util.Optional;

public class GetUserByIdService implements GetUserByIdQuery {

    private final LoadUserAuthPort loadUserAuthPort;

    public GetUserByIdService(LoadUserAuthPort loadUserAuthPort) {
        this.loadUserAuthPort = Objects.requireNonNull(loadUserAuthPort);
    }

    @Override
    public Optional<User> execute(UserId id, AuthenticatedActor actor) {
        Objects.requireNonNull(id, "El ID de usuario no puede ser null");
        Objects.requireNonNull(actor, "El actor no puede ser null");

        if (actor.role() != Role.ADMIN && !actor.userId().equals(id)) {
            throw new UnauthorizedOperationException(actor.role(), "obtener detalle de otro usuario");
        }

        return loadUserAuthPort.loadById(id);
    }
}
