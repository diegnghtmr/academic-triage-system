package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.command.request.AssignRequestCommand;

public interface AssignRequestUseCase extends AuthenticatedRequestUseCase<AssignRequestCommand, RequestSummary> {
}
