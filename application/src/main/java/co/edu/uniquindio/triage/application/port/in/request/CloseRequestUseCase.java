package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.command.request.CloseRequestCommand;

public interface CloseRequestUseCase extends AuthenticatedRequestUseCase<CloseRequestCommand, RequestSummary> {
}
