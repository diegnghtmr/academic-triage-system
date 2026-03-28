package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.CancelRequestCommand;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort;
import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.InvalidStateTransitionException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancelRequestServiceTest {

    private StubLoadRequestPort loadRequestPort;
    private StubLoadRequestTypePort loadRequestTypePort;
    private StubLoadOriginChannelPort loadOriginChannelPort;
    private StubLoadUserAuthPort loadUserAuthPort;
    private CapturingSaveRequestPort saveRequestPort;
    private CancelRequestService service;

    @BeforeEach
    void setUp() {
        loadRequestPort = new StubLoadRequestPort();
        loadRequestTypePort = new StubLoadRequestTypePort();
        loadOriginChannelPort = new StubLoadOriginChannelPort();
        loadUserAuthPort = new StubLoadUserAuthPort();
        saveRequestPort = new CapturingSaveRequestPort();
        service = new CancelRequestService(
                loadRequestPort,
                loadRequestTypePort,
                loadOriginChannelPort,
                loadUserAuthPort,
                saveRequestPort
        );
    }

    @Test
    void ownerStudentMustCancelEligibleRequestAndReturnReconstructedSummary() {
        var requester = persistedUser(7L, "student.owner", Role.STUDENT, true);
        var actor = new AuthenticatedActor(requester.getId(), requester.getUsername().value(), Role.STUDENT);
        var requestType = new RequestType(new RequestTypeId(4L), "Homologación", "Cambio de tipo", true);
        var originChannel = new OriginChannel(new OriginChannelId(2L), "Correo", true);
        var request = classifiedRequest(requester.getId(), requestType.getId(), originChannel.getId(), new UserId(15L));
        loadRequestPort.store(request);
        loadRequestTypePort.store(requestType);
        loadOriginChannelPort.store(originChannel);
        loadUserAuthPort.store(requester);

        var result = service.execute(
                new CancelRequestCommand(request.getId(), "  Ya resolví este trámite por otra vía  "),
                actor
        );

        assertThat(result.request().getStatus()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(result.request().getCancellationReason()).isEqualTo("Ya resolví este trámite por otra vía");
        assertThat(result.request().getHistory()).hasSize(3);
        assertThat(result.request().getHistory().getLast().getAction()).isEqualTo(HistoryAction.CANCELLED);
        assertThat(result.request().getHistory().getLast().getPerformedById()).isEqualTo(actor.userId());
        assertThat(result.request().getHistory().getLast().getObservations()).isEqualTo("Ya resolví este trámite por otra vía");
        assertThat(result.requestType()).isEqualTo(requestType);
        assertThat(result.originChannel()).isEqualTo(originChannel);
        assertThat(result.requester()).isEqualTo(requester);
        assertThat(result.assignedTo()).isEmpty();
        assertThat(saveRequestPort.saved()).containsSame(request);
    }

    @Test
    void staffAndAdminMustCancelWithoutOwnershipRequirement() {
        var requester = persistedUser(7L, "student.owner", Role.STUDENT, true);
        var requestType = new RequestType(new RequestTypeId(4L), "Homologación", "Cambio de tipo", true);
        var originChannel = new OriginChannel(new OriginChannelId(2L), "Correo", true);
        loadRequestTypePort.store(requestType);
        loadOriginChannelPort.store(originChannel);
        loadUserAuthPort.store(requester);

        var staffRequest = registeredRequest(new RequestId(42L), requester.getId(), requestType.getId(), originChannel.getId());
        loadRequestPort.store(staffRequest);
        var staffActor = new AuthenticatedActor(new UserId(90L), "staff.actor", Role.STAFF);

        var staffResult = service.execute(
                new CancelRequestCommand(staffRequest.getId(), "Motivo válido para staff"),
                staffActor
        );

        assertThat(staffResult.request().getStatus()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(staffResult.request().getHistory().getLast().getPerformedById()).isEqualTo(staffActor.userId());

        var adminRequest = registeredRequest(new RequestId(43L), requester.getId(), requestType.getId(), originChannel.getId());
        loadRequestPort.store(adminRequest);
        var adminActor = new AuthenticatedActor(new UserId(91L), "admin.actor", Role.ADMIN);

        var adminResult = service.execute(
                new CancelRequestCommand(adminRequest.getId(), "Motivo válido para admin"),
                adminActor
        );

        assertThat(adminResult.request().getStatus()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(adminResult.request().getHistory().getLast().getPerformedById()).isEqualTo(adminActor.userId());
        assertThat(saveRequestPort.saved()).containsSame(adminRequest);
    }

    @Test
    void nonOwnerStudentMustNotCancelExistingRequest() {
        var requester = persistedUser(7L, "student.owner", Role.STUDENT, true);
        var outsider = new AuthenticatedActor(new UserId(99L), "student.other", Role.STUDENT);
        var request = registeredRequest(new RequestId(42L), requester.getId(), new RequestTypeId(4L), new OriginChannelId(2L));
        loadRequestPort.store(request);

        assertThatThrownBy(() -> service.execute(
                new CancelRequestCommand(request.getId(), "Motivo válido"),
                outsider
        )).isInstanceOf(UnauthorizedOperationException.class);

        assertThat(saveRequestPort.saved()).isEmpty();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REGISTERED);
    }

    @Test
    void missingRequestMustRaiseNotFoundBeforeAuthorization() {
        var actor = new AuthenticatedActor(new UserId(99L), "student.other", Role.STUDENT);

        assertThatThrownBy(() -> service.execute(
                new CancelRequestCommand(new RequestId(404L), "Motivo válido"),
                actor
        )).isInstanceOf(RequestNotFoundException.class);

        assertThat(saveRequestPort.saved()).isEmpty();
    }

    @Test
    void cancelMustPropagateLifecycleConflictWhenRequestIsNotCancellable() {
        var requester = persistedUser(7L, "student.owner", Role.STUDENT, true);
        var actor = new AuthenticatedActor(requester.getId(), requester.getUsername().value(), Role.STUDENT);
        var request = inProgressRequest(requester.getId(), persistedUser(15L, "staff.owner", Role.STAFF, true), new RequestTypeId(4L), new OriginChannelId(2L), new UserId(15L));
        loadRequestPort.store(request);

        assertThatThrownBy(() -> service.execute(
                new CancelRequestCommand(request.getId(), "Motivo válido"),
                actor
        )).isInstanceOf(InvalidStateTransitionException.class);

        assertThat(saveRequestPort.saved()).isEmpty();
    }

    private static AcademicRequest registeredRequest(RequestId requestId,
                                                     UserId applicantId,
                                                     RequestTypeId requestTypeId,
                                                     OriginChannelId originChannelId) {
        return new AcademicRequest(
                requestId,
                "Necesito resolver esta solicitud académica antes del cierre del semestre",
                applicantId,
                originChannelId,
                requestTypeId,
                null,
                false,
                LocalDateTime.now()
        );
    }

    private static AcademicRequest classifiedRequest(UserId applicantId,
                                                     RequestTypeId requestTypeId,
                                                     OriginChannelId originChannelId,
                                                     UserId performedBy) {
        var request = registeredRequest(new RequestId(42L), applicantId, requestTypeId, originChannelId);
        request.classify(requestTypeId, "Clasificación inicial", performedBy, LocalDateTime.now());
        return request;
    }

    private static AcademicRequest inProgressRequest(UserId applicantId,
                                                     User assignee,
                                                     RequestTypeId requestTypeId,
                                                     OriginChannelId originChannelId,
                                                     UserId performedBy) {
        var request = classifiedRequest(applicantId, requestTypeId, originChannelId, performedBy);
        request.prioritize(Priority.HIGH, "Riesgo de perder el semestre", performedBy, LocalDateTime.now());
        request.assign(assignee, performedBy, "Asignada para atención", LocalDateTime.now());
        return request;
    }

    private static User persistedUser(long id, String username, Role role, boolean active) {
        return User.reconstitute(
                new UserId(id),
                new Username(username),
                "Usuario",
                "Persistido",
                new PasswordHash("encoded-password"),
                new Identification("ID-" + id),
                new Email(username + "@uniquindio.edu.co"),
                role,
                active
        );
    }

    private static final class StubLoadRequestPort implements LoadRequestPort {
        private final Map<Long, AcademicRequest> requests = new HashMap<>();

        @Override
        public Optional<AcademicRequest> loadById(RequestId requestId) {
            return Optional.ofNullable(requests.get(requestId.value()));
        }

        @Override
        public Optional<RequestDetail> loadDetailById(RequestId requestId) {
            return Optional.empty();
        }

        void store(AcademicRequest request) {
            requests.put(request.getId().value(), request);
        }
    }

    private static final class StubLoadRequestTypePort implements LoadRequestTypePort {
        private final Map<Long, RequestType> requestTypes = new HashMap<>();

        @Override
        public Optional<RequestType> loadById(RequestTypeId requestTypeId) {
            return Optional.ofNullable(requestTypes.get(requestTypeId.value()));
        }

        void store(RequestType requestType) {
            requestTypes.put(requestType.getId().value(), requestType);
        }
    }

    private static final class StubLoadOriginChannelPort implements LoadOriginChannelPort {
        private final Map<Long, OriginChannel> channels = new HashMap<>();

        @Override
        public Optional<OriginChannel> loadById(OriginChannelId originChannelId) {
            return Optional.ofNullable(channels.get(originChannelId.value()));
        }

        void store(OriginChannel channel) {
            channels.put(channel.getId().value(), channel);
        }
    }

    private static final class StubLoadUserAuthPort implements LoadUserAuthPort {
        private final Map<Long, User> users = new HashMap<>();

        @Override
        public Optional<User> loadByUsername(Username username) {
            return users.values().stream().filter(user -> user.getUsername().equals(username)).findFirst();
        }

        @Override
        public Optional<User> loadByEmail(Email email) {
            return users.values().stream().filter(user -> user.getEmail().equals(email)).findFirst();
        }

        @Override
        public Optional<User> loadById(UserId id) {
            return Optional.ofNullable(users.get(id.value()));
        }

        @Override
        public boolean existsByUsername(Username username) {
            return false;
        }

        @Override
        public boolean existsByEmail(Email email) {
            return false;
        }

        @Override
        public boolean existsByIdentification(Identification identification) {
            return false;
        }

        void store(User user) {
            users.put(user.getId().value(), user);
        }
    }

    private static final class CapturingSaveRequestPort implements SaveRequestPort {
        private AcademicRequest saved;

        @Override
        public void save(AcademicRequest request) {
            this.saved = request;
        }

        Optional<AcademicRequest> saved() {
            return Optional.ofNullable(saved);
        }
    }
}
