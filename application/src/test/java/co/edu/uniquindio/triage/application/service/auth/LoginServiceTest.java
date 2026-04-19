package co.edu.uniquindio.triage.application.service.auth;

import co.edu.uniquindio.triage.application.port.in.command.LoginCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.security.AuthToken;
import co.edu.uniquindio.triage.application.port.out.security.AuthenticatedUserPayload;
import co.edu.uniquindio.triage.application.port.out.security.PasswordEncoderPort;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.AmbiguousLoginIdentifierException;
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

    // ── canonical identifier (username) success ──────────────────────────────

    @Test
    void canonicalUsernameIdentifierMustIssueToken() {
        userPort.usernameResult = persistedUser(1L, "jperez", "jperez@uniquindio.edu.co", true, "encoded-MyPassword123");

        var result = loginService.login(new LoginCommand("jperez", "MyPassword123"));

        assertThat(result.authToken().token()).isEqualTo("jwt-token");
        assertThat(result.authToken().tokenType()).isEqualTo("Bearer");
        assertThat(result.user().getUsername().value()).isEqualTo("jperez");
    }

    // ── canonical identifier (email) success ─────────────────────────────────

    @Test
    void canonicalEmailIdentifierMustIssueToken() {
        userPort.emailResult = persistedUser(2L, "alopez", "alopez@uniquindio.edu.co", true, "encoded-MyPassword123");

        var result = loginService.login(new LoginCommand("alopez@uniquindio.edu.co", "MyPassword123"));

        assertThat(result.user().getUsername().value()).isEqualTo("alopez");
    }

    // ── deprecated alias (username field) success ─────────────────────────────

    @Test
    void aliasPathMustSucceedWithParityToCanonical() {
        userPort.usernameResult = persistedUser(1L, "jperez", "jperez@uniquindio.edu.co", true, "encoded-MyPassword123");

        var result = loginService.login(new LoginCommand("jperez", true, "MyPassword123"));

        assertThat(result.authToken().token()).isEqualTo("jwt-token");
        assertThat(result.user().getUsername().value()).isEqualTo("jperez");
    }

    // ── invalid credentials parity ────────────────────────────────────────────

    @Test
    void wrongPasswordMustFailWithAuthenticationFailed() {
        userPort.usernameResult = persistedUser(1L, "jperez", "jperez@uniquindio.edu.co", true, "encoded-MyPassword123");

        assertThatThrownBy(() -> loginService.login(new LoginCommand("jperez", "wrongPass123")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void unknownIdentifierMustFailWithAuthenticationFailed() {
        assertThatThrownBy(() -> loginService.login(new LoginCommand("noexiste", "MyPassword123")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    // ── inactive user parity ──────────────────────────────────────────────────

    @Test
    void inactiveUserWithUsernameIdentifierMustFailAuthentication() {
        userPort.usernameResult = persistedUser(1L, "jperez", "jperez@uniquindio.edu.co", false, "encoded-MyPassword123");

        assertThatThrownBy(() -> loginService.login(new LoginCommand("jperez", "MyPassword123")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    @Test
    void inactiveUserWithEmailIdentifierMustFailAuthentication() {
        userPort.emailResult = persistedUser(2L, "alopez", "alopez@uniquindio.edu.co", false, "encoded-MyPassword123");

        assertThatThrownBy(() -> loginService.login(new LoginCommand("alopez@uniquindio.edu.co", "MyPassword123")))
                .isInstanceOf(AuthenticationFailedException.class);
    }

    // ── both fields precedence (same logical value) ───────────────────────────

    @Test
    void bothFieldsSameValueMustSucceedUsingCanonicalSemantics() {
        userPort.usernameResult = persistedUser(1L, "jperez", "jperez@uniquindio.edu.co", true, "encoded-MyPassword123");

        // LoginCommand with isAlias=false represents the canonical identifier path
        var result = loginService.login(new LoginCommand("jperez", false, "MyPassword123"));

        assertThat(result.user().getUsername().value()).isEqualTo("jperez");
    }

    // ── ambiguity handling ────────────────────────────────────────────────────

    @Test
    void ambiguousIdentifierThatMatchesBothUsernameAndEmailForDifferentUsersMustReject() {
        // Simulates a data-quality situation: "shared@uniquindio.edu.co" is a username for user 1
        // AND also the email for a different user 2 → two distinct candidates
        userPort.usernameResult = persistedUser(1L, "shared@uniquindio.edu.co", "other1@test.com", true, "encoded-pass");
        userPort.emailResult = persistedUser(2L, "another", "shared@uniquindio.edu.co", true, "encoded-pass");

        assertThatThrownBy(() -> loginService.login(new LoginCommand("shared@uniquindio.edu.co", "password123")))
                .isInstanceOf(AmbiguousLoginIdentifierException.class);
    }

    @Test
    void sameUserFoundByBothUsernameAndEmailLookupMustNotBeAmbiguous() {
        // Same userId deduplication: if username lookup and email lookup return the same user, it's 1 candidate
        var user = persistedUser(1L, "jperez@uniquindio.edu.co", "jperez@uniquindio.edu.co", true, "encoded-MyPassword123");
        userPort.usernameResult = user;
        userPort.emailResult = user;

        var result = loginService.login(new LoginCommand("jperez@uniquindio.edu.co", "MyPassword123"));

        assertThat(result.user().getUsername().value()).isEqualTo("jperez@uniquindio.edu.co");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User persistedUser(long id, String username, String email, boolean active, String hash) {
        return User.reconstitute(
                new UserId(id),
                new Username(username),
                "Juan",
                "Pérez",
                new PasswordHash(hash),
                new Identification("1094123456"),
                new Email(email),
                Role.STUDENT,
                active
        );
    }

    // ── stubs ─────────────────────────────────────────────────────────────────

    private static final class StubLoadUserAuthPort implements LoadUserAuthPort {
        private User usernameResult;
        private User emailResult;

        @Override
        public Optional<User> loadByUsername(Username username) {
            return Optional.ofNullable(usernameResult);
        }

        @Override
        public Optional<User> loadByEmail(Email email) {
            return Optional.ofNullable(emailResult);
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
