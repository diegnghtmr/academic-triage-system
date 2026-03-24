package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.GetRequestDetailQueryModel;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetRequestDetailServiceTest {

    @Test
    void studentMustNotAccessAnotherUsersRequest() {
        var detail = detailForApplicant(55L);
        var service = new GetRequestDetailService(new StubLoadRequestPort(detail));
        var actor = new AuthenticatedActor(new UserId(7L), "jperez", Role.STUDENT);

        assertThatThrownBy(() -> service.execute(new GetRequestDetailQueryModel(new RequestId(42L)), actor))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void missingRequestMustReturnNotFound() {
        var service = new GetRequestDetailService(new StubLoadRequestPort(null));
        var actor = new AuthenticatedActor(new UserId(7L), "jperez", Role.STAFF);

        assertThatThrownBy(() -> service.execute(new GetRequestDetailQueryModel(new RequestId(42L)), actor))
                .isInstanceOf(RequestNotFoundException.class);
    }

    @Test
    void staffMayReadExistingRequestDetail() {
        var detail = detailForApplicant(55L);
        var service = new GetRequestDetailService(new StubLoadRequestPort(detail));
        var actor = new AuthenticatedActor(new UserId(7L), "staff", Role.STAFF);

        var result = service.execute(new GetRequestDetailQueryModel(new RequestId(42L)), actor);

        assertThat(result).isSameAs(detail);
        assertThat(result.history()).isEmpty();
    }

    private static RequestDetail detailForApplicant(long applicantId) {
        var requester = User.reconstitute(
                new UserId(applicantId),
                new Username("requester" + applicantId),
                "Ana",
                "García",
                new PasswordHash("encoded-password"),
                new Identification("ID-" + applicantId),
                new Email("requester" + applicantId + "@uniquindio.edu.co"),
                Role.STUDENT,
                true
        );
        var request = new AcademicRequest(
                new RequestId(42L),
                "Necesito un cupo adicional para la materia",
                requester.getId(),
                new OriginChannelId(2L),
                new RequestTypeId(3L),
                null,
                false,
                LocalDateTime.now()
        );
        return new RequestDetail(
                request,
                new RequestType(new RequestTypeId(3L), "Cupo", "Solicitud de cupo", true),
                new OriginChannel(new OriginChannelId(2L), "Correo", true),
                requester,
                Optional.empty(),
                List.of()
        );
    }

    private record StubLoadRequestPort(RequestDetail detail) implements LoadRequestPort {

        @Override
        public Optional<AcademicRequest> loadById(RequestId requestId) {
            return Optional.ofNullable(detail).map(RequestDetail::request);
        }

        @Override
        public Optional<RequestDetail> loadDetailById(RequestId requestId) {
            return Optional.ofNullable(detail);
        }
    }
}
