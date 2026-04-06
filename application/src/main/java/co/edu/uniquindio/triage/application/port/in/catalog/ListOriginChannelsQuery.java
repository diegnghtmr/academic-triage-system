package co.edu.uniquindio.triage.application.port.in.catalog;

import co.edu.uniquindio.triage.application.port.in.command.catalog.ListOriginChannelsQueryModel;
import co.edu.uniquindio.triage.application.port.in.request.AuthenticatedRequestQuery;
import co.edu.uniquindio.triage.domain.model.OriginChannel;

import java.util.List;

public interface ListOriginChannelsQuery extends AuthenticatedRequestQuery<ListOriginChannelsQueryModel, List<OriginChannel>> {
}
