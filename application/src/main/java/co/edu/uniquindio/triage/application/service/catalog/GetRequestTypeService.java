package co.edu.uniquindio.triage.application.service.catalog;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.catalog.GetRequestTypeQuery;
import co.edu.uniquindio.triage.application.port.in.command.catalog.GetRequestTypeQueryModel;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.RequestType;

import java.util.Objects;

public class GetRequestTypeService implements GetRequestTypeQuery {

    private final LoadRequestTypePort loadRequestTypePort;

    public GetRequestTypeService(LoadRequestTypePort loadRequestTypePort) {
        this.loadRequestTypePort = Objects.requireNonNull(loadRequestTypePort, "El loadRequestTypePort no puede ser null");
    }

    @Override
    public RequestType execute(GetRequestTypeQueryModel query, AuthenticatedActor actor) {
        Objects.requireNonNull(query, "El query no puede ser null");
        CatalogAuthorizationSupport.ensureAuthenticatedActor(actor);
        return loadRequestTypePort.loadById(query.requestTypeId())
                .orElseThrow(() -> new EntityNotFoundException("RequestType", "id", query.requestTypeId().value()));
    }
}
