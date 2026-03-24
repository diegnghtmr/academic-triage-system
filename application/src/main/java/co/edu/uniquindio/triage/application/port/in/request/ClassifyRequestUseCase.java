package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.command.request.ClassifyRequestCommand;

public interface ClassifyRequestUseCase extends AuthenticatedRequestUseCase<ClassifyRequestCommand, RequestSummary> {
}
