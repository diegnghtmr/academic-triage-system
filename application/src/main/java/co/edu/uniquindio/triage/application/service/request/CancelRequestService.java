package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.CancelRequestCommand;
import co.edu.uniquindio.triage.application.port.in.request.CancelRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestForMutationPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class CancelRequestService implements CancelRequestUseCase {

    private final LoadRequestForMutationPort loadRequestForMutationPort;
    private final LoadRequestTypePort loadRequestTypePort;
    private final LoadOriginChannelPort loadOriginChannelPort;
    private final LoadUserAuthPort loadUserAuthPort;
    private final SaveRequestPort saveRequestPort;

    public CancelRequestService(LoadRequestForMutationPort loadRequestForMutationPort,
                                LoadRequestTypePort loadRequestTypePort,
                                LoadOriginChannelPort loadOriginChannelPort,
                                LoadUserAuthPort loadUserAuthPort,
                                SaveRequestPort saveRequestPort) {
        this.loadRequestForMutationPort = Objects.requireNonNull(loadRequestForMutationPort, "El loadRequestForMutationPort no puede ser null");
        this.loadRequestTypePort = Objects.requireNonNull(loadRequestTypePort, "El loadRequestTypePort no puede ser null");
        this.loadOriginChannelPort = Objects.requireNonNull(loadOriginChannelPort, "El loadOriginChannelPort no puede ser null");
        this.loadUserAuthPort = Objects.requireNonNull(loadUserAuthPort, "El loadUserAuthPort no puede ser null");
        this.saveRequestPort = Objects.requireNonNull(saveRequestPort, "El saveRequestPort no puede ser null");
    }

    @Override
    public RequestSummary execute(CancelRequestCommand command, AuthenticatedActor actor) {
        Objects.requireNonNull(command, "El command no puede ser null");
        Objects.requireNonNull(actor, "El actor no puede ser null");

        var request = loadRequestForMutationPort.loadByIdForMutation(command.requestId())
                .orElseThrow(() -> new RequestNotFoundException(command.requestId()));

        ensureAuthorizedActor(actor, request.getApplicantId());

        request.cancel(command.cancellationReason(), actor.userId(), LocalDateTime.now());
        saveRequestPort.save(request);

        return new RequestSummary(
                request,
                loadRequestTypePort.loadById(request.getRequestTypeId())
                        .orElseThrow(() -> new EntityNotFoundException("RequestType", "id", request.getRequestTypeId().value())),
                loadOriginChannelPort.loadById(request.getOriginChannelId())
                        .orElseThrow(() -> new EntityNotFoundException("OriginChannel", "id", request.getOriginChannelId().value())),
                loadUserAuthPort.loadById(request.getApplicantId())
                        .orElseThrow(() -> new EntityNotFoundException("User", "id", request.getApplicantId().value())),
                Optional.ofNullable(request.getResponsibleId())
                        .flatMap(loadUserAuthPort::loadById)
        );
    }

    private void ensureAuthorizedActor(AuthenticatedActor actor, UserId applicantId) {
        if (actor.role() == Role.STAFF || actor.role() == Role.ADMIN) {
            return;
        }

        if (actor.role() == Role.STUDENT && actor.userId().equals(applicantId)) {
            return;
        }

        throw new UnauthorizedOperationException(actor.role(), "cancel request");
    }
}
