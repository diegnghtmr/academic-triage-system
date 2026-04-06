package co.edu.uniquindio.triage.application.service.user;

import co.edu.uniquindio.triage.application.port.in.user.GetUserByIdQuery;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
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
    public Optional<User> execute(UserId id) {
        return loadUserAuthPort.loadById(id);
    }
}
