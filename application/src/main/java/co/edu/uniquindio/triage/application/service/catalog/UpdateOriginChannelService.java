package co.edu.uniquindio.triage.application.service.catalog;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.catalog.UpdateOriginChannelUseCase;
import co.edu.uniquindio.triage.application.port.in.command.catalog.UpdateOriginChannelCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveOriginChannelPort;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.OriginChannel;

import java.util.Objects;

public class UpdateOriginChannelService implements UpdateOriginChannelUseCase {

    private final LoadOriginChannelPort loadOriginChannelPort;
    private final SaveOriginChannelPort saveOriginChannelPort;

    public UpdateOriginChannelService(LoadOriginChannelPort loadOriginChannelPort,
                                      SaveOriginChannelPort saveOriginChannelPort) {
        this.loadOriginChannelPort = Objects.requireNonNull(loadOriginChannelPort, "El loadOriginChannelPort no puede ser null");
        this.saveOriginChannelPort = Objects.requireNonNull(saveOriginChannelPort, "El saveOriginChannelPort no puede ser null");
    }

    @Override
    public OriginChannel execute(UpdateOriginChannelCommand command, AuthenticatedActor actor) {
        Objects.requireNonNull(command, "El command no puede ser null");
        CatalogAuthorizationSupport.ensureAdminActor(actor, "update origin channel");

        var originChannel = loadOriginChannelPort.loadById(command.originChannelId())
                .orElseThrow(() -> new EntityNotFoundException("OriginChannel", "id", command.originChannelId().value()));

        ensureNameAvailable(command);
        originChannel.updateName(command.name());
        return saveOriginChannelPort.saveOriginChannel(originChannel);
    }

    private void ensureNameAvailable(UpdateOriginChannelCommand command) {
        if (loadOriginChannelPort.existsOriginChannelByNameIgnoreCaseAndIdNot(command.name(), command.originChannelId())) {
            throw new DuplicateCatalogEntryException("canal de origen", command.name());
        }
    }
}
