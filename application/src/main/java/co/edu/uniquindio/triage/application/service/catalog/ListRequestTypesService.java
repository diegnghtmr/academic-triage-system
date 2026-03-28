package co.edu.uniquindio.triage.application.service.catalog;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.catalog.ListRequestTypesQuery;
import co.edu.uniquindio.triage.application.port.in.command.catalog.ListRequestTypesQueryModel;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.domain.model.RequestType;

import java.util.List;
import java.util.Objects;

public class ListRequestTypesService implements ListRequestTypesQuery {

    private final LoadRequestTypePort loadRequestTypePort;

    public ListRequestTypesService(LoadRequestTypePort loadRequestTypePort) {
        this.loadRequestTypePort = Objects.requireNonNull(loadRequestTypePort, "El loadRequestTypePort no puede ser null");
    }

    @Override
    public List<RequestType> execute(ListRequestTypesQueryModel query, AuthenticatedActor actor) {
        Objects.requireNonNull(query, "El query no puede ser null");
        CatalogAuthorizationSupport.ensureAuthenticatedActor(actor);
        return loadRequestTypePort.loadAllRequestTypes(query.active());
    }
}
