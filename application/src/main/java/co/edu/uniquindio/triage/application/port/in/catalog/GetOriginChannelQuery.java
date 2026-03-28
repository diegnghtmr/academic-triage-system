package co.edu.uniquindio.triage.application.port.in.catalog;

import co.edu.uniquindio.triage.application.port.in.command.catalog.GetOriginChannelQueryModel;
import co.edu.uniquindio.triage.application.port.in.request.AuthenticatedRequestQuery;
import co.edu.uniquindio.triage.domain.model.OriginChannel;

public interface GetOriginChannelQuery extends AuthenticatedRequestQuery<GetOriginChannelQueryModel, OriginChannel> {
}
