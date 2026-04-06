package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.command.request.CreateRequestCommand;

public interface CreateRequestUseCase extends AuthenticatedRequestUseCase<CreateRequestCommand, RequestSummary> {
}
