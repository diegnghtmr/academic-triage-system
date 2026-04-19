package co.edu.uniquindio.triage.application.service.catalog;

import co.edu.uniquindio.triage.application.port.in.catalog.GetRequestTypeVersionUseCase;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadCatalogVersionPort;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.util.Objects;
import java.util.Optional;

public class GetRequestTypeVersionService implements GetRequestTypeVersionUseCase {

    private final LoadCatalogVersionPort loadCatalogVersionPort;

    public GetRequestTypeVersionService(LoadCatalogVersionPort loadCatalogVersionPort) {
        this.loadCatalogVersionPort = Objects.requireNonNull(loadCatalogVersionPort,
                "loadCatalogVersionPort no puede ser null");
    }

    @Override
    public Optional<Long> getVersionById(RequestTypeId id) {
        Objects.requireNonNull(id, "El id no puede ser null");
        return loadCatalogVersionPort.findRequestTypeVersionById(id);
    }
}
