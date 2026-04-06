package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.command.request.ListRequestsQueryModel;
import co.edu.uniquindio.triage.application.port.in.common.Page;

public interface ListRequestsQuery extends AuthenticatedRequestQuery<ListRequestsQueryModel, Page<RequestSummary>> {
}
