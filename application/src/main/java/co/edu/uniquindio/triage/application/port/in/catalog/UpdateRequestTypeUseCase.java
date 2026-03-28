package co.edu.uniquindio.triage.application.port.in.catalog;

import co.edu.uniquindio.triage.application.port.in.command.catalog.UpdateRequestTypeCommand;
import co.edu.uniquindio.triage.application.port.in.request.AuthenticatedRequestUseCase;
import co.edu.uniquindio.triage.domain.model.RequestType;

public interface UpdateRequestTypeUseCase extends AuthenticatedRequestUseCase<UpdateRequestTypeCommand, RequestType> {
}
