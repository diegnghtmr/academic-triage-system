package co.edu.uniquindio.triage.application.service.catalog;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.catalog.UpdateRequestTypeUseCase;
import co.edu.uniquindio.triage.application.port.in.command.catalog.UpdateRequestTypeCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestTypePort;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.RequestType;

import java.util.Objects;

public class UpdateRequestTypeService implements UpdateRequestTypeUseCase {

    private final LoadRequestTypePort loadRequestTypePort;
    private final SaveRequestTypePort saveRequestTypePort;

    public UpdateRequestTypeService(LoadRequestTypePort loadRequestTypePort,
                                    SaveRequestTypePort saveRequestTypePort) {
        this.loadRequestTypePort = Objects.requireNonNull(loadRequestTypePort, "El loadRequestTypePort no puede ser null");
        this.saveRequestTypePort = Objects.requireNonNull(saveRequestTypePort, "El saveRequestTypePort no puede ser null");
    }

    @Override
    public RequestType execute(UpdateRequestTypeCommand command, AuthenticatedActor actor) {
        Objects.requireNonNull(command, "El command no puede ser null");
        CatalogAuthorizationSupport.ensureAdminActor(actor, "update request type");

        var requestType = loadRequestTypePort.loadById(command.requestTypeId())
                .orElseThrow(() -> new EntityNotFoundException("RequestType", "id", command.requestTypeId().value()));

        ensureNameAvailable(command);
        requestType.updateName(command.name());
        requestType.updateDescription(command.description());
        return saveRequestTypePort.saveRequestType(requestType);
    }

    private void ensureNameAvailable(UpdateRequestTypeCommand command) {
        if (loadRequestTypePort.existsRequestTypeByNameIgnoreCaseAndIdNot(command.name(), command.requestTypeId())) {
            throw new DuplicateCatalogEntryException("tipo de solicitud", command.name());
        }
    }
}
