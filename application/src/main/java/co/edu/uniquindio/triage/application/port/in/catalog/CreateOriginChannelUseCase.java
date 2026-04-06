package co.edu.uniquindio.triage.application.port.in.catalog;

import co.edu.uniquindio.triage.application.port.in.command.catalog.CreateOriginChannelCommand;
import co.edu.uniquindio.triage.application.port.in.request.AuthenticatedRequestUseCase;
import co.edu.uniquindio.triage.domain.model.OriginChannel;

public interface CreateOriginChannelUseCase extends AuthenticatedRequestUseCase<CreateOriginChannelCommand, OriginChannel> {
}
