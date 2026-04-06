package co.edu.uniquindio.triage.application.service.catalog;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.catalog.ListOriginChannelsQuery;
import co.edu.uniquindio.triage.application.port.in.command.catalog.ListOriginChannelsQueryModel;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.domain.model.OriginChannel;

import java.util.List;
import java.util.Objects;

public class ListOriginChannelsService implements ListOriginChannelsQuery {

    private final LoadOriginChannelPort loadOriginChannelPort;

    public ListOriginChannelsService(LoadOriginChannelPort loadOriginChannelPort) {
        this.loadOriginChannelPort = Objects.requireNonNull(loadOriginChannelPort, "El loadOriginChannelPort no puede ser null");
    }

    @Override
    public List<OriginChannel> execute(ListOriginChannelsQueryModel query, AuthenticatedActor actor) {
        Objects.requireNonNull(query, "El query no puede ser null");
        CatalogAuthorizationSupport.ensureAuthenticatedActor(actor);
        return loadOriginChannelPort.loadAllOriginChannels(query.active());
    }
}
