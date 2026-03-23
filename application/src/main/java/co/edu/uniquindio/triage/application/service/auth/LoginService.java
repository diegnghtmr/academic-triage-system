package co.edu.uniquindio.triage.application.service.auth;

import co.edu.uniquindio.triage.application.port.in.auth.AuthResult;
import co.edu.uniquindio.triage.application.port.in.auth.LoginUseCase;
import co.edu.uniquindio.triage.application.port.in.command.LoginCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.security.PasswordEncoderPort;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.domain.exception.AuthenticationFailedException;

import java.util.Objects;

public class LoginService implements LoginUseCase {

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

        var user = loadUserAuthPort.loadByUsername(command.username())
                .orElseThrow(AuthenticationFailedException::new);

        if (!user.isActive()) {
            throw new AuthenticationFailedException();
        }
        if (!passwordEncoderPort.matches(command.rawPassword(), user.getPasswordHash().value())) {
            throw new AuthenticationFailedException();
        }

        return new AuthResult(tokenProviderPort.issue(user), user);
    }
}
