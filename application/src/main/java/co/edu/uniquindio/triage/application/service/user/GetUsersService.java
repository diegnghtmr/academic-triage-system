package co.edu.uniquindio.triage.application.service.user;

import co.edu.uniquindio.triage.application.port.in.common.Page;
import co.edu.uniquindio.triage.application.port.in.user.GetUsersQuery;
import co.edu.uniquindio.triage.application.port.in.user.command.GetUsersQueryModel;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUsersPort;
import co.edu.uniquindio.triage.domain.model.User;

import java.util.Objects;

public class GetUsersService implements GetUsersQuery {

    private final LoadUsersPort loadUsersPort;

    public GetUsersService(LoadUsersPort loadUsersPort) {
        this.loadUsersPort = Objects.requireNonNull(loadUsersPort);
    }

    @Override
    public Page<User> execute(GetUsersQueryModel query) {
        return loadUsersPort.loadAll(query);
    }
}
