package co.edu.uniquindio.triage.application.port.in.catalog;

import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.util.Optional;

public interface GetRequestTypeVersionUseCase {
    Optional<Long> getVersionById(RequestTypeId id);
}
