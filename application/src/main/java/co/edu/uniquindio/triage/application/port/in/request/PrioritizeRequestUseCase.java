package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.command.request.PrioritizeRequestCommand;

public interface PrioritizeRequestUseCase extends AuthenticatedRequestUseCase<PrioritizeRequestCommand, RequestSummary> {
}
