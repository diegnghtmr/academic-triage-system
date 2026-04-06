package co.edu.uniquindio.triage.application.service.auth;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.RegisterCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveUserPort;
import co.edu.uniquindio.triage.application.port.out.security.PasswordEncoderPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.DuplicateUserException;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterServiceTest {

    private InMemoryUserAuthPort userPort;
    private RegisterService registerService;

    @BeforeEach
    void setUp() {
        userPort = new InMemoryUserAuthPort();
        registerService = new RegisterService(userPort, userPort, new StubPasswordEncoderPort());
    }

    @Test
    void anonymousRegistrationMustBePersistedAsStudent() {
        var command = new RegisterCommand(
                new Username("jperez"),
                new Email("jperez@uniquindio.edu.co"),
                "MyPassword123",
                "Juan",
                "Pérez",
                new Identification("1094123456"),
                Role.ADMIN
        );

        var user = registerService.register(command, Optional.empty());

        assertThat(user.getRole()).isEqualTo(Role.STUDENT);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getPasswordHash().value()).isEqualTo("encoded-MyPassword123");
    }

    @Test
    void adminRegistrationCanAssignStaffRole() {
        var command = new RegisterCommand(
                new Username("staff01"),
                new Email("staff01@uniquindio.edu.co"),
                "MyPassword123",
                "Staff",
                "Uno",
                new Identification("20001"),
                Role.STAFF
        );

        var actor = Optional.of(new AuthenticatedActor(new UserId(99L), "admin", Role.ADMIN));
        var user = registerService.register(command, actor);

        assertThat(user.getRole()).isEqualTo(Role.STAFF);
        assertThat(userPort.loadByIdCalls()).isZero();
    }

    @Test
    void duplicateUsernameMustBeRejected() {
        var existing = persistedUser(1L, "jperez", "jperez@uniquindio.edu.co", "1094123456");
        userPort.save(existing);

        var command = new RegisterCommand(
                new Username("jperez"),
                new Email("otro@uniquindio.edu.co"),
                "MyPassword123",
                "Juan",
                "Duplicado",
                new Identification("99999"),
                null
        );

        assertThatThrownBy(() -> registerService.register(command, Optional.empty()))
                .isInstanceOf(DuplicateUserException.class)
                .hasMessageContaining("username");
    }

    private User persistedUser(Long id, String username, String email, String identification) {
        return User.reconstitute(
                new UserId(id),
                new Username(username),
                "Nombre",
                "Persistido",
                new PasswordHash("encoded-MyPassword123"),
                new Identification(identification),
                new Email(email),
                Role.STUDENT,
                true
        );
    }

    private static final class StubPasswordEncoderPort implements PasswordEncoderPort {
        @Override
        public String encode(String rawPassword) {
            return "encoded-" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            return encodedPassword.equals(encode(rawPassword));
        }
    }

    private static final class InMemoryUserAuthPort implements LoadUserAuthPort, SaveUserPort {

        private final AtomicLong sequence = new AtomicLong(1);
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

        @Override
        public User save(User user) {
            var userId = user.getId().isPresent() ? user.getId().orElseThrow().value() : sequence.getAndIncrement();
            var persisted = User.reconstitute(
                    new UserId(userId),
                    user.getUsername(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getPasswordHash(),
                    user.getIdentification(),
                    user.getEmail(),
                    user.getRole(),
                    user.isActive()
            );
            users.put(userId, persisted);
            return persisted;
        }

        int loadByIdCalls() {
            return loadByIdCalls.get();
        }
    }
}
