package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.application.port.in.common.Page;
import co.edu.uniquindio.triage.domain.model.User;

public interface LoadUsersPort {
    Page<User> loadAll(UserSearchCriteria criteria);
}
