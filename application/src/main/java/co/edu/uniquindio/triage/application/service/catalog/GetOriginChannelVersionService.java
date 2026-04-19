package co.edu.uniquindio.triage.application.service.catalog;

import co.edu.uniquindio.triage.application.port.in.catalog.GetOriginChannelVersionUseCase;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadCatalogVersionPort;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;

import java.util.Objects;
import java.util.Optional;

public class GetOriginChannelVersionService implements GetOriginChannelVersionUseCase {

    private final LoadCatalogVersionPort loadCatalogVersionPort;

    public GetOriginChannelVersionService(LoadCatalogVersionPort loadCatalogVersionPort) {
        this.loadCatalogVersionPort = Objects.requireNonNull(loadCatalogVersionPort,
                "loadCatalogVersionPort no puede ser null");
    }

    @Override
    public Optional<Long> getVersionById(OriginChannelId id) {
        Objects.requireNonNull(id, "El id no puede ser null");
        return loadCatalogVersionPort.findOriginChannelVersionById(id);
    }
}
