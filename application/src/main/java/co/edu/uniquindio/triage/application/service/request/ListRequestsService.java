package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.ListRequestsQueryModel;
import co.edu.uniquindio.triage.application.port.in.common.Page;
import co.edu.uniquindio.triage.application.port.in.request.ListRequestsQuery;
import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;
import co.edu.uniquindio.triage.application.port.out.persistence.RequestSearchCriteria;
import co.edu.uniquindio.triage.application.port.out.persistence.SearchRequestPort;
import co.edu.uniquindio.triage.domain.enums.Role;

import java.util.Objects;

public class ListRequestsService implements ListRequestsQuery {

    private final SearchRequestPort searchRequestPort;

    public ListRequestsService(SearchRequestPort searchRequestPort) {
        this.searchRequestPort = Objects.requireNonNull(searchRequestPort, "El searchRequestPort no puede ser null");
    }

    @Override
    public Page<RequestSummary> execute(ListRequestsQueryModel query, AuthenticatedActor actor) {
        Objects.requireNonNull(query, "El query no puede ser null");
        Objects.requireNonNull(actor, "El actor no puede ser null");

        var effectiveRequesterId = actor.role() == Role.STUDENT
                ? java.util.Optional.of(actor.userId())
                : query.requesterUserId();

        var criteria = new RequestSearchCriteria(
                query.status(),
                query.requestTypeId(),
                query.priority(),
                query.assignedToUserId(),
                effectiveRequesterId,
                query.dateFrom(),
                query.dateTo(),
                query.page(),
                query.size(),
                query.sort()
        );

        return searchRequestPort.search(criteria);
    }
}
