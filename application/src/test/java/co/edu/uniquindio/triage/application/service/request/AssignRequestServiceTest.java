package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.AssignRequestCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestForMutationPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort;
import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
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

class AssignRequestServiceTest {

    private StubLoadRequestPort loadRequestPort;
    private StubLoadRequestTypePort loadRequestTypePort;
    private StubLoadOriginChannelPort loadOriginChannelPort;
    private StubLoadUserAuthPort loadUserAuthPort;
    private CapturingSaveRequestPort saveRequestPort;
    private AssignRequestService service;

    @BeforeEach
    void setUp() {
        loadRequestPort = new StubLoadRequestPort();
        loadRequestTypePort = new StubLoadRequestTypePort();
        loadOriginChannelPort = new StubLoadOriginChannelPort();
        loadUserAuthPort = new StubLoadUserAuthPort();
        saveRequestPort = new CapturingSaveRequestPort();
        service = new AssignRequestService(
                loadRequestPort,
                loadRequestTypePort,
                loadOriginChannelPort,
                loadUserAuthPort,
                saveRequestPort
        );
    }

    @Test
    void staffMustAssignPrioritizedRequestAndPersistResponsibleUser() {
        var actor = new AuthenticatedActor(new UserId(9L), "staff", Role.STAFF);
        var requester = persistedUser(7L, "student", Role.STUDENT, true);
        var assignee = persistedUser(15L, "staff.owner", Role.STAFF, true);
        var requestType = new RequestType(new RequestTypeId(4L), "Homologación", "Cambio de tipo", true);
        var originChannel = new OriginChannel(new OriginChannelId(2L), "Correo", true);
        var request = prioritizedRequest(requester.getId().orElseThrow(), requestType.getId(), originChannel.getId(), actor.userId());
        loadRequestPort.store(request);
        loadRequestTypePort.store(requestType);
        loadOriginChannelPort.store(originChannel);
        loadUserAuthPort.store(requester);
        loadUserAuthPort.store(assignee);

        var result = service.execute(
                new AssignRequestCommand(request.getId(), assignee.getId().orElseThrow(), "  Asignada a mesa especializada  "),
                actor
        );

        assertThat(result.request().getStatus()).isEqualTo(RequestStatus.IN_PROGRESS);
        assertThat(result.request().getResponsibleId()).isEqualTo(assignee.getId().orElseThrow());
        assertThat(result.request().getHistory()).hasSize(4);
        assertThat(result.request().getHistory().getLast().getAction()).isEqualTo(HistoryAction.ASSIGNED);
        assertThat(result.request().getHistory().getLast().getPerformedById()).isEqualTo(actor.userId());
        assertThat(result.request().getHistory().getLast().getResponsibleId()).isEqualTo(assignee.getId().orElseThrow());
        assertThat(result.request().getHistory().getLast().getObservations()).isEqualTo("Asignada a mesa especializada");
        assertThat(result.requestType()).isEqualTo(requestType);
        assertThat(result.originChannel()).isEqualTo(originChannel);
        assertThat(result.requester()).isEqualTo(requester);
        assertThat(result.assignedTo()).contains(assignee);
        assertThat(saveRequestPort.saved()).containsSame(request);
    }

    @Test
    void nonStaffActorsMustNotAssignRequests() {
        var actor = new AuthenticatedActor(new UserId(1L), "admin", Role.ADMIN);

        assertThatThrownBy(() -> service.execute(
                new AssignRequestCommand(new RequestId(42L), new UserId(15L), null),
                actor
        )).isInstanceOf(UnauthorizedOperationException.class);

        assertThat(saveRequestPort.saved()).isEmpty();
    }

    @Test
    void missingRequestMustRaiseNotFound() {
        var actor = new AuthenticatedActor(new UserId(9L), "staff", Role.STAFF);

        assertThatThrownBy(() -> service.execute(
                new AssignRequestCommand(new RequestId(404L), new UserId(15L), null),
                actor
        )).isInstanceOf(RequestNotFoundException.class);

        assertThat(saveRequestPort.saved()).isEmpty();
    }

    @Test
    void missingAssigneeMustRaiseEntityNotFound() {
        var actor = new AuthenticatedActor(new UserId(9L), "staff", Role.STAFF);
        var requester = persistedUser(7L, "student", Role.STUDENT, true);
        var requestType = new RequestType(new RequestTypeId(4L), "Homologación", "Cambio de tipo", true);
        var originChannel = new OriginChannel(new OriginChannelId(2L), "Correo", true);
        var request = prioritizedRequest(requester.getId().orElseThrow(), requestType.getId(), originChannel.getId(), actor.userId());
        loadRequestPort.store(request);

        assertThatThrownBy(() -> service.execute(
                new AssignRequestCommand(request.getId(), new UserId(99L), null),
                actor
        )).isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User");

        assertThat(saveRequestPort.saved()).isEmpty();
    }

    @Test
    void inactiveAssigneeMustBeRejected() {
        var actor = new AuthenticatedActor(new UserId(9L), "staff", Role.STAFF);
        var requester = persistedUser(7L, "student", Role.STUDENT, true);
        var assignee = persistedUser(15L, "staff.owner", Role.STAFF, false);
        var requestType = new RequestType(new RequestTypeId(4L), "Homologación", "Cambio de tipo", true);
        var originChannel = new OriginChannel(new OriginChannelId(2L), "Correo", true);
        var request = prioritizedRequest(requester.getId().orElseThrow(), requestType.getId(), originChannel.getId(), actor.userId());
        loadRequestPort.store(request);
        loadUserAuthPort.store(assignee);

        assertThatThrownBy(() -> service.execute(
                new AssignRequestCommand(request.getId(), assignee.getId().orElseThrow(), null),
                actor
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("activo");

        assertThat(saveRequestPort.saved()).isEmpty();
    }

    @Test
    void nonStaffAssigneeMustBeRejected() {
        var actor = new AuthenticatedActor(new UserId(9L), "staff", Role.STAFF);
        var requester = persistedUser(7L, "student", Role.STUDENT, true);
        var assignee = persistedUser(15L, "student.owner", Role.STUDENT, true);
        var requestType = new RequestType(new RequestTypeId(4L), "Homologación", "Cambio de tipo", true);
        var originChannel = new OriginChannel(new OriginChannelId(2L), "Correo", true);
        var request = prioritizedRequest(requester.getId().orElseThrow(), requestType.getId(), originChannel.getId(), actor.userId());
        loadRequestPort.store(request);
        loadUserAuthPort.store(assignee);

        assertThatThrownBy(() -> service.execute(
                new AssignRequestCommand(request.getId(), assignee.getId().orElseThrow(), null),
                actor
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rol STAFF");

        assertThat(saveRequestPort.saved()).isEmpty();
    }

    private static AcademicRequest prioritizedRequest(UserId applicantId,
                                                      RequestTypeId requestTypeId,
                                                      OriginChannelId originChannelId,
                                                      UserId performedBy) {
        var request = new AcademicRequest(
                new RequestId(42L),
                "Necesito asignar responsable a esta solicitud académica priorizada",
                applicantId,
                originChannelId,
                requestTypeId,
                null,
                false,
                LocalDateTime.now()
        );
        request.classify(requestTypeId, "Clasificación inicial", performedBy, LocalDateTime.now());
        request.prioritize(Priority.HIGH, "Riesgo de perder el semestre", performedBy, LocalDateTime.now());
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

    private static final class StubLoadRequestPort implements LoadRequestPort, LoadRequestForMutationPort {
        private final Map<Long, AcademicRequest> requests = new HashMap<>();

        @Override
        public Optional<AcademicRequest> loadById(RequestId requestId) {
            return Optional.ofNullable(requests.get(requestId.value()));
        }

        @Override
        public Optional<AcademicRequest> loadByIdForMutation(RequestId requestId) {
            return loadById(requestId);
        }

        @Override
        public Optional<co.edu.uniquindio.triage.application.port.in.request.RequestDetail> loadDetailById(RequestId requestId) {
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
            users.put(user.getId().orElseThrow().value(), user);
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
