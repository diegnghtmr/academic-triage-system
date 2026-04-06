package co.edu.uniquindio.triage.application.port.in.auth;

import co.edu.uniquindio.triage.application.port.in.command.LoginCommand;

public interface LoginUseCase {

    AuthResult login(LoginCommand command);
}
