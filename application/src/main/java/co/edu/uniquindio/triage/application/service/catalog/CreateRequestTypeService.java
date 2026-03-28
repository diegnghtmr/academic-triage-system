package co.edu.uniquindio.triage.application.service.catalog;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.catalog.CreateRequestTypeUseCase;
import co.edu.uniquindio.triage.application.port.in.command.catalog.CreateRequestTypeCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestTypePort;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.model.RequestType;

import java.util.Objects;

public class CreateRequestTypeService implements CreateRequestTypeUseCase {

    private final LoadRequestTypePort loadRequestTypePort;
    private final SaveRequestTypePort saveRequestTypePort;

    public CreateRequestTypeService(LoadRequestTypePort loadRequestTypePort,
                                    SaveRequestTypePort saveRequestTypePort) {
        this.loadRequestTypePort = Objects.requireNonNull(loadRequestTypePort, "El loadRequestTypePort no puede ser null");
        this.saveRequestTypePort = Objects.requireNonNull(saveRequestTypePort, "El saveRequestTypePort no puede ser null");
    }

    @Override
    public RequestType execute(CreateRequestTypeCommand command, AuthenticatedActor actor) {
        Objects.requireNonNull(command, "El command no puede ser null");
        CatalogAuthorizationSupport.ensureAdminActor(actor, "create request type");

        ensureNameAvailable(command.name());
        return saveRequestTypePort.saveRequestType(RequestType.createNew(command.name(), command.description()));
    }

    private void ensureNameAvailable(String name) {
        if (loadRequestTypePort.existsRequestTypeByNameIgnoreCase(name)) {
            throw new DuplicateCatalogEntryException("tipo de solicitud", name);
        }
    }
}
