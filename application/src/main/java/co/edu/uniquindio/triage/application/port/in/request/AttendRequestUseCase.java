package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.command.request.AttendRequestCommand;

public interface AttendRequestUseCase extends AuthenticatedRequestUseCase<AttendRequestCommand, RequestSummary> {
}
