package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.command.request.CancelRequestCommand;

public interface CancelRequestUseCase extends AuthenticatedRequestUseCase<CancelRequestCommand, RequestSummary> {
}
