package co.edu.uniquindio.triage.application.service.auth;

import co.edu.uniquindio.triage.application.port.in.auth.AuthResult;
import co.edu.uniquindio.triage.application.port.in.auth.LoginUseCase;
import co.edu.uniquindio.triage.application.port.in.command.LoginCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.security.PasswordEncoderPort;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.domain.exception.AmbiguousLoginIdentifierException;
import co.edu.uniquindio.triage.domain.exception.AuthenticationFailedException;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class LoginService implements LoginUseCase {

    private static final Logger log = Logger.getLogger(LoginService.class.getName());

    private final LoadUserAuthPort loadUserAuthPort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final TokenProviderPort tokenProviderPort;

    public LoginService(LoadUserAuthPort loadUserAuthPort,
                        PasswordEncoderPort passwordEncoderPort,
                        TokenProviderPort tokenProviderPort) {
        this.loadUserAuthPort = Objects.requireNonNull(loadUserAuthPort);
        this.passwordEncoderPort = Objects.requireNonNull(passwordEncoderPort);
        this.tokenProviderPort = Objects.requireNonNull(tokenProviderPort);
    }

    @Override
    public AuthResult login(LoginCommand command) {
        Objects.requireNonNull(command, "El command no puede ser null");

        if (command.isAlias()) {
            log.warning("Login via deprecated 'username' alias field — migrate to 'identifier'");
        }

        var user = resolveUniqueUser(command.identifier());

        if (!user.isActive()) {
            throw new AuthenticationFailedException();
        }
        if (!passwordEncoderPort.matches(command.rawPassword(), user.getPasswordHash().value())) {
            throw new AuthenticationFailedException();
        }

        return new AuthResult(tokenProviderPort.issue(user), user);
    }

    /**
     * Builds a deduplicated candidate set by querying username and, when the identifier
     * looks like an email, also querying by email. Returns the sole match or throws:
     * <ul>
     *   <li>{@link AuthenticationFailedException} when no user found (0 candidates)</li>
     *   <li>{@link AmbiguousLoginIdentifierException} when more than one distinct user found</li>
     * </ul>
     */
    private User resolveUniqueUser(String identifier) {
        Map<Long, User> candidates = new LinkedHashMap<>();

        loadUserAuthPort.loadByUsername(new Username(identifier))
                .ifPresent(u -> u.getId().ifPresent(id -> candidates.put(id.value(), u)));

        if (looksLikeEmail(identifier)) {
            try {
                loadUserAuthPort.loadByEmail(new Email(identifier))
                        .ifPresent(u -> u.getId().ifPresent(id -> candidates.put(id.value(), u)));
            } catch (IllegalArgumentException ignored) {
                // invalid email format — username-only lookup already performed
            }
        }

        return switch (candidates.size()) {
            case 0 -> throw new AuthenticationFailedException();
            case 1 -> candidates.values().iterator().next();
            default -> throw new AmbiguousLoginIdentifierException();
        };
    }

    private boolean looksLikeEmail(String value) {
        return value != null && value.contains("@");
    }
}
