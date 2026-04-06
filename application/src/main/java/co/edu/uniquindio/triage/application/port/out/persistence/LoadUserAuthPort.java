package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.util.Optional;

public interface LoadUserAuthPort {

    Optional<User> loadByUsername(Username username);

    Optional<User> loadByEmail(Email email);

    Optional<User> loadById(UserId id);

    boolean existsByUsername(Username username);

    boolean existsByEmail(Email email);

    boolean existsByIdentification(Identification identification);
}
