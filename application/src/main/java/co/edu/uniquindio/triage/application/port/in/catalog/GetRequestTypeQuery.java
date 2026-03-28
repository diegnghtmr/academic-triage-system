package co.edu.uniquindio.triage.application.port.in.catalog;

import co.edu.uniquindio.triage.application.port.in.command.catalog.GetRequestTypeQueryModel;
import co.edu.uniquindio.triage.application.port.in.request.AuthenticatedRequestQuery;
import co.edu.uniquindio.triage.domain.model.RequestType;

public interface GetRequestTypeQuery extends AuthenticatedRequestQuery<GetRequestTypeQueryModel, RequestType> {
}
