package co.edu.uniquindio.triage.application.port.in.user;

import co.edu.uniquindio.triage.application.port.in.common.Page;
import co.edu.uniquindio.triage.application.port.in.user.command.GetUsersQueryModel;
import co.edu.uniquindio.triage.domain.model.User;

public interface GetUsersQuery {
    Page<User> execute(GetUsersQueryModel query);
}
