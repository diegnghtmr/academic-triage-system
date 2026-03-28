package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.command.request.RejectRequestCommand;

public interface RejectRequestUseCase extends AuthenticatedRequestUseCase<RejectRequestCommand, RequestSummary> {
}
