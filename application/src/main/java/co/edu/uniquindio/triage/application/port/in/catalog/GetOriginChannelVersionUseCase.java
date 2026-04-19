package co.edu.uniquindio.triage.application.port.in.catalog;

import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;

import java.util.Optional;

public interface GetOriginChannelVersionUseCase {
    Optional<Long> getVersionById(OriginChannelId id);
}
