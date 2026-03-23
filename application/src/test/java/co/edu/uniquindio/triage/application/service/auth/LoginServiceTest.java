package co.edu.uniquindio.triage.application.service.auth;

import co.edu.uniquindio.triage.application.port.in.command.LoginCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.security.AuthToken;
import co.edu.uniquindio.triage.application.port.out.security.AuthenticatedUserPayload;
import co.edu.uniquindio.triage.application.port.out.security.PasswordEncoderPort;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.AuthenticationFailedException;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginServiceTest {

    private LoginService loginService;
    private StubLoadUserAuthPort userPort;

    @BeforeEach
    void setUp() {
        userPort = new StubLoadUserAuthPort();
        loginService = new LoginService(userPort, new StubPasswordEncoderPort(), new StubTokenProviderPort());
    }

    @Test
    void validCredentialsMustIssueJwtResponse() {
        userPort.user = persistedUser(true, "encoded-MyPassword123");

        var result = loginService.login(new LoginCommand(new Username("jperez"), "MyPassword123"));

        assertThat(result.authToken().token()).isEqualTo("jwt-token");
        assertThat(result.authToken().tokenType()).isEqualTo("Bearer");
        assertThat(result.user().getUsername().value()).isEqualTo("jperez");
    }

    @Test
    void wrongPasswordMustFailAuthentication() {
        userPort.user = persistedUser(true, "encoded-MyPassword123");

        assertThatThrownBy(() -> loginService.login(new LoginCommand(new Username("jperez"), "wrongPass123")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void inactiveUserMustFailAuthentication() {
        userPort.user = persistedUser(false, "encoded-MyPassword123");

        assertThatThrownBy(() -> loginService.login(new LoginCommand(new Username("jperez"), "MyPassword123")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    private User persistedUser(boolean active, String hash) {
        return User.reconstitute(
                new UserId(1L),
                new Username("jperez"),
                "Juan Pérez",
                new PasswordHash(hash),
                new Identification("1094123456"),
                new Email("jperez@uniquindio.edu.co"),
                Role.STUDENT,
                active
        );
    }

    private static final class StubLoadUserAuthPort implements LoadUserAuthPort {
        private User user;

        @Override
        public Optional<User> loadByUsername(Username username) {
            return Optional.ofNullable(user);
        }

        @Override
        public Optional<User> loadByEmail(Email email) {
            return Optional.empty();
        }

        @Override
        public Optional<User> loadById(UserId id) {
            return Optional.empty();
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

    private static final class StubTokenProviderPort implements TokenProviderPort {
        @Override
        public AuthToken issue(User user) {
            return new AuthToken("jwt-token", "Bearer", 86400);
        }

        @Override
        public AuthenticatedUserPayload parse(String token) {
            return new AuthenticatedUserPayload(1L, "jperez", Role.STUDENT);
        }
    }
}
