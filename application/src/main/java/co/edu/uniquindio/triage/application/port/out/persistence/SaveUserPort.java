package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.User;

public interface SaveUserPort {

    User save(User user);
}
