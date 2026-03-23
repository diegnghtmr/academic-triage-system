package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.request.AuthenticatedRequestUseCase;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.UserNotActiveException;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedRequestActorBoundaryTest {

    private TrackingLoadUserAuthPort loadUserAuthPort;
    private HybridActorBoundaryUseCase useCase;

    @BeforeEach
    void setUp() {
        loadUserAuthPort = new TrackingLoadUserAuthPort();
        useCase = new HybridActorBoundaryUseCase(loadUserAuthPort);
    }

    @Test
    void coarseAuthorizationMustUseMinimalAuthenticatedActorWithoutRehydration() {
        var actor = new AuthenticatedActor(new UserId(41L), "student", Role.STUDENT);

        var decision = useCase.execute(new RequestBoundaryCommand(new UserId(41L), false), actor);

        assertThat(decision.authorized()).isTrue();
        assertThat(decision.usedCanonicalUser()).isFalse();
        assertThat(loadUserAuthPort.loadByIdCalls()).isZero();
    }

    @Test
    void mutableUserInvariantMustRehydrateCanonicalUserOnlyWhenRequired() {
        var actor = new AuthenticatedActor(new UserId(42L), "staff", Role.STAFF);
        loadUserAuthPort.store(persistedUser(42L, "staff", Role.STAFF, false));

        assertThatThrownBy(() -> useCase.execute(new RequestBoundaryCommand(new UserId(7L), true), actor))
                .isInstanceOf(UserNotActiveException.class);

        assertThat(loadUserAuthPort.loadByIdCalls()).isEqualTo(1);
    }

    private static User persistedUser(Long id, String username, Role role, boolean active) {
        return User.reconstitute(
                new UserId(id),
                new Username(username),
                "Usuario Persistido",
                new PasswordHash("encoded-password"),
                new Identification("ID-" + id),
                new Email(username + "@uniquindio.edu.co"),
                role,
                active
        );
    }

    private record RequestBoundaryCommand(UserId applicantId, boolean requiresCurrentActorState) {
    }

    private record RequestBoundaryDecision(boolean authorized, boolean usedCanonicalUser) {
    }

    private static final class HybridActorBoundaryUseCase
            implements AuthenticatedRequestUseCase<RequestBoundaryCommand, RequestBoundaryDecision> {

        private final LoadUserAuthPort loadUserAuthPort;

        private HybridActorBoundaryUseCase(LoadUserAuthPort loadUserAuthPort) {
            this.loadUserAuthPort = loadUserAuthPort;
        }

        @Override
        public RequestBoundaryDecision execute(RequestBoundaryCommand command, AuthenticatedActor actor) {
            if (!command.requiresCurrentActorState()) {
                var authorized = actor.role() == Role.STUDENT && actor.userId().equals(command.applicantId());
                return new RequestBoundaryDecision(authorized, false);
            }

            var canonicalActor = loadUserAuthPort.loadById(actor.userId())
                    .orElseThrow(() -> new IllegalStateException("Actor must exist when invariant requires current state"));
            canonicalActor.ensureActive();
            return new RequestBoundaryDecision(canonicalActor.canClassifyRequest(), true);
        }
    }

    private static final class TrackingLoadUserAuthPort implements LoadUserAuthPort {

        private final AtomicInteger loadByIdCalls = new AtomicInteger();
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
            loadByIdCalls.incrementAndGet();
            return Optional.ofNullable(users.get(id.value()));
        }

        @Override
        public boolean existsByUsername(Username username) {
            return loadByUsername(username).isPresent();
        }

        @Override
        public boolean existsByEmail(Email email) {
            return loadByEmail(email).isPresent();
        }

        @Override
        public boolean existsByIdentification(Identification identification) {
            return users.values().stream().anyMatch(user -> user.getIdentification().equals(identification));
        }

        void store(User user) {
            users.put(user.getId().value(), user);
        }

        int loadByIdCalls() {
            return loadByIdCalls.get();
        }
    }
}
