package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.command.request.ListRequestsQueryModel;

public interface ListRequestsQuery extends AuthenticatedRequestQuery<ListRequestsQueryModel, RequestPage<RequestSummary>> {
}
