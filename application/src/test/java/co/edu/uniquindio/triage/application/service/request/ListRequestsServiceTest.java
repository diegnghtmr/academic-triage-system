package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.ListRequestsQueryModel;
import co.edu.uniquindio.triage.application.port.in.request.RequestPage;
import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;
import co.edu.uniquindio.triage.application.port.out.persistence.RequestSearchCriteria;
import co.edu.uniquindio.triage.application.port.out.persistence.SearchRequestPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ListRequestsServiceTest {

    @Test
    void studentScopeMustAlwaysBeForcedToAuthenticatedRequester() {
        var searchPort = new CapturingSearchRequestPort();
        var service = new ListRequestsService(searchPort);
        var actor = new AuthenticatedActor(new UserId(7L), "jperez", Role.STUDENT);

        var result = service.execute(
                new ListRequestsQueryModel(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(new UserId(99L)),
                        Optional.empty(),
                        Optional.empty(),
                        0,
                        20,
                        "registrationDateTime,desc"
                ),
                actor
        );

        assertThat(result).isSameAs(searchPort.result);
        assertThat(searchPort.criteria.requesterUserId()).contains(actor.userId());
    }

    @Test
    void staffScopeMustPreserveRequestedFilters() {
        var searchPort = new CapturingSearchRequestPort();
        var service = new ListRequestsService(searchPort);
        var actor = new AuthenticatedActor(new UserId(12L), "staff", Role.STAFF);
        var requesterFilter = new UserId(44L);

        service.execute(
                new ListRequestsQueryModel(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(requesterFilter),
                        Optional.empty(),
                        Optional.empty(),
                        1,
                        10,
                        "registrationDateTime,desc"
                ),
                actor
        );

        assertThat(searchPort.criteria.requesterUserId()).contains(requesterFilter);
        assertThat(searchPort.criteria.page()).isEqualTo(1);
        assertThat(searchPort.criteria.size()).isEqualTo(10);
    }

    private static final class CapturingSearchRequestPort implements SearchRequestPort {
        private RequestSearchCriteria criteria;
        private final RequestPage<RequestSummary> result = new RequestPage<>(java.util.List.of(), 0, 0, 0, 20);

        @Override
        public RequestPage<RequestSummary> search(RequestSearchCriteria criteria) {
            this.criteria = criteria;
            return result;
        }
    }
}
