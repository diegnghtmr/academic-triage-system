package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.CreateRequestCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.NextRequestIdPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort;
import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Role;
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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateRequestServiceTest {

    private StubLoadRequestTypePort loadRequestTypePort;
    private StubLoadOriginChannelPort loadOriginChannelPort;
    private StubLoadUserAuthPort loadUserAuthPort;
    private CapturingSaveRequestPort saveRequestPort;
    private CreateRequestService service;

    @BeforeEach
    void setUp() {
        loadRequestTypePort = new StubLoadRequestTypePort();
        loadOriginChannelPort = new StubLoadOriginChannelPort();
        loadUserAuthPort = new StubLoadUserAuthPort();
        saveRequestPort = new CapturingSaveRequestPort();
        service = new CreateRequestService(
                new FixedNextRequestIdPort(new RequestId(42L)),
                loadRequestTypePort,
                loadOriginChannelPort,
                loadUserAuthPort,
                saveRequestPort
        );
    }

    @Test
    void studentMustCreateRegisteredRequestWithInitialHistory() {
        var actor = new AuthenticatedActor(new UserId(7L), "jperez", Role.STUDENT);
        var requester = persistedUser(7L, "jperez", Role.STUDENT, true);
        var requestType = new RequestType(new RequestTypeId(3L), "Cupo", "Solicitud de cupo", true);
        var originChannel = new OriginChannel(new OriginChannelId(2L), "Correo", true);
        loadUserAuthPort.store(requester);
        loadRequestTypePort.store(requestType);
        loadOriginChannelPort.store(originChannel);

        var result = service.execute(
                new CreateRequestCommand(requestType.getId(), originChannel.getId(), "Necesito un cupo adicional para la materia", LocalDate.of(2026, 3, 15)),
                actor
        );

        assertThat(result.request().getId()).isEqualTo(new RequestId(42L));
        assertThat(result.request().getStatus()).isEqualTo(RequestStatus.REGISTERED);
        assertThat(result.request().getHistory()).hasSize(1);
        assertThat(result.request().getHistory().getFirst().getAction()).isEqualTo(HistoryAction.REGISTERED);
        assertThat(result.requester()).isEqualTo(requester);
        assertThat(result.requestType()).isEqualTo(requestType);
        assertThat(result.originChannel()).isEqualTo(originChannel);
        assertThat(result.assignedTo()).isEmpty();
        assertThat(saveRequestPort.saved()).containsSame(result.request());
    }

    @Test
    void adminMustNotCreateRequests() {
        var actor = new AuthenticatedActor(new UserId(9L), "admin", Role.ADMIN);

        assertThatThrownBy(() -> service.execute(
                new CreateRequestCommand(new RequestTypeId(1L), new OriginChannelId(1L), "Descripción válida para la solicitud", null),
                actor
        )).isInstanceOf(UnauthorizedOperationException.class);

        assertThat(saveRequestPort.saved()).isEmpty();
    }

    @Test
    void inactiveCatalogReferencesMustBeRejected() {
        var actor = new AuthenticatedActor(new UserId(7L), "jperez", Role.STUDENT);
        loadUserAuthPort.store(persistedUser(7L, "jperez", Role.STUDENT, true));
        loadRequestTypePort.store(new RequestType(new RequestTypeId(3L), "Cupo", "Solicitud de cupo", false));
        loadOriginChannelPort.store(new OriginChannel(new OriginChannelId(2L), "Correo", true));

        assertThatThrownBy(() -> service.execute(
                new CreateRequestCommand(new RequestTypeId(3L), new OriginChannelId(2L), "Necesito un cupo adicional para la materia", null),
                actor
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipo de solicitud");

        assertThat(saveRequestPort.saved()).isEmpty();
    }

    private static User persistedUser(long id, String username, Role role, boolean active) {
        return User.reconstitute(
                new UserId(id),
                new Username(username),
                "Juan",
                "Pérez",
                new PasswordHash("encoded-password"),
                new Identification("ID-" + id),
                new Email(username + "@uniquindio.edu.co"),
                role,
                active
        );
    }

    private record FixedNextRequestIdPort(RequestId requestId) implements NextRequestIdPort {
        @Override
        public RequestId nextId() {
            return requestId;
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
