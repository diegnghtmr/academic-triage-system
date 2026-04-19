package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.Optional;

public interface LoadRequestVersionPort {
    Optional<Long> findVersionById(RequestId id);
}
