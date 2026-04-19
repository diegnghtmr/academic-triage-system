package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.AssignRequestCommand;
import co.edu.uniquindio.triage.application.port.in.request.AssignRequestUseCase;
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

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class AssignRequestService implements AssignRequestUseCase {

    private final LoadRequestForMutationPort loadRequestForMutationPort;
    private final LoadRequestTypePort loadRequestTypePort;
    private final LoadOriginChannelPort loadOriginChannelPort;
    private final LoadUserAuthPort loadUserAuthPort;
    private final SaveRequestPort saveRequestPort;

    public AssignRequestService(LoadRequestForMutationPort loadRequestForMutationPort,
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
    public RequestSummary execute(AssignRequestCommand command, AuthenticatedActor actor) {
        Objects.requireNonNull(command, "El command no puede ser null");
        Objects.requireNonNull(actor, "El actor no puede ser null");

        ensureStaffActor(actor);

        var request = loadRequestForMutationPort.loadByIdForMutation(command.requestId())
                .orElseThrow(() -> new RequestNotFoundException(command.requestId()));
        var assignee = loadUserAuthPort.loadById(command.assignedToUserId())
                .orElseThrow(() -> new EntityNotFoundException("User", "id", command.assignedToUserId().value()));

        request.assign(assignee, actor.userId(), command.observations(), LocalDateTime.now());
        saveRequestPort.save(request);

        return new RequestSummary(
                request,
                loadRequestTypePort.loadById(request.getRequestTypeId())
                        .orElseThrow(() -> new EntityNotFoundException("RequestType", "id", request.getRequestTypeId().value())),
                loadOriginChannelPort.loadById(request.getOriginChannelId())
                        .orElseThrow(() -> new EntityNotFoundException("OriginChannel", "id", request.getOriginChannelId().value())),
                loadUserAuthPort.loadById(request.getApplicantId())
                        .orElseThrow(() -> new EntityNotFoundException("User", "id", request.getApplicantId().value())),
                Optional.of(assignee)
        );
    }

    private void ensureStaffActor(AuthenticatedActor actor) {
        if (actor.role() != Role.STAFF) {
            throw new UnauthorizedOperationException(actor.role(), "assign request");
        }
    }
}
