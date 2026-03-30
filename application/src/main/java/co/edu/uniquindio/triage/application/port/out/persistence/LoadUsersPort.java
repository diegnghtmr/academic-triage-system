package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.application.port.in.common.Page;
import co.edu.uniquindio.triage.application.port.in.user.command.GetUsersQueryModel;
import co.edu.uniquindio.triage.domain.model.User;

public interface LoadUsersPort {
    Page<User> loadAll(GetUsersQueryModel query);
}
