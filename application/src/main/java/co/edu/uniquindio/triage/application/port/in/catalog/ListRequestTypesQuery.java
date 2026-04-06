package co.edu.uniquindio.triage.application.port.in.catalog;

import co.edu.uniquindio.triage.application.port.in.command.catalog.ListRequestTypesQueryModel;
import co.edu.uniquindio.triage.application.port.in.request.AuthenticatedRequestQuery;
import co.edu.uniquindio.triage.domain.model.RequestType;

import java.util.List;

public interface ListRequestTypesQuery extends AuthenticatedRequestQuery<ListRequestTypesQueryModel, List<RequestType>> {
}
