package co.edu.uniquindio.triage.application.service.catalog;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.catalog.GetOriginChannelQuery;
import co.edu.uniquindio.triage.application.port.in.command.catalog.GetOriginChannelQueryModel;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.OriginChannel;

import java.util.Objects;

public class GetOriginChannelService implements GetOriginChannelQuery {

    private final LoadOriginChannelPort loadOriginChannelPort;

    public GetOriginChannelService(LoadOriginChannelPort loadOriginChannelPort) {
        this.loadOriginChannelPort = Objects.requireNonNull(loadOriginChannelPort, "El loadOriginChannelPort no puede ser null");
    }

    @Override
    public OriginChannel execute(GetOriginChannelQueryModel query, AuthenticatedActor actor) {
        Objects.requireNonNull(query, "El query no puede ser null");
        CatalogAuthorizationSupport.ensureAuthenticatedActor(actor);
        return loadOriginChannelPort.loadById(query.originChannelId())
                .orElseThrow(() -> new EntityNotFoundException("OriginChannel", "id", query.originChannelId().value()));
    }
}
