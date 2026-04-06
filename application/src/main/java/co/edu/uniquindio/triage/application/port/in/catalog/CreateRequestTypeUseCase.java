package co.edu.uniquindio.triage.application.port.in.catalog;

import co.edu.uniquindio.triage.application.port.in.command.catalog.CreateRequestTypeCommand;
import co.edu.uniquindio.triage.application.port.in.request.AuthenticatedRequestUseCase;
import co.edu.uniquindio.triage.domain.model.RequestType;

public interface CreateRequestTypeUseCase extends AuthenticatedRequestUseCase<CreateRequestTypeCommand, RequestType> {
}
