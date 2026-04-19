package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.util.Optional;

public interface LoadUserVersionPort {
    Optional<Long> findVersionById(UserId id);
}
