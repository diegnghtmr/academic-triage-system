package co.edu.uniquindio.triage.application.service.catalog;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.catalog.CreateOriginChannelUseCase;
import co.edu.uniquindio.triage.application.port.in.command.catalog.CreateOriginChannelCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveOriginChannelPort;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.model.OriginChannel;

import java.util.Objects;

public class CreateOriginChannelService implements CreateOriginChannelUseCase {

    private final LoadOriginChannelPort loadOriginChannelPort;
    private final SaveOriginChannelPort saveOriginChannelPort;

    public CreateOriginChannelService(LoadOriginChannelPort loadOriginChannelPort,
                                      SaveOriginChannelPort saveOriginChannelPort) {
        this.loadOriginChannelPort = Objects.requireNonNull(loadOriginChannelPort, "El loadOriginChannelPort no puede ser null");
        this.saveOriginChannelPort = Objects.requireNonNull(saveOriginChannelPort, "El saveOriginChannelPort no puede ser null");
    }

    @Override
    public OriginChannel execute(CreateOriginChannelCommand command, AuthenticatedActor actor) {
        Objects.requireNonNull(command, "El command no puede ser null");
        CatalogAuthorizationSupport.ensureAdminActor(actor, "create origin channel");

        ensureNameAvailable(command.name());
        return saveOriginChannelPort.saveOriginChannel(OriginChannel.createNew(command.name()));
    }

    private void ensureNameAvailable(String name) {
        if (loadOriginChannelPort.existsOriginChannelByNameIgnoreCase(name)) {
            throw new DuplicateCatalogEntryException("canal de origen", name);
        }
    }
}
