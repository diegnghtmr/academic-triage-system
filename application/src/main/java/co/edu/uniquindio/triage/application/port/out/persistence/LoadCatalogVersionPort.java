package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.util.Optional;

public interface LoadCatalogVersionPort {
    Optional<Long> findRequestTypeVersionById(RequestTypeId id);
    Optional<Long> findOriginChannelVersionById(OriginChannelId id);
}
