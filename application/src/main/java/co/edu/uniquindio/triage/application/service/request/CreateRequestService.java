package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.CreateRequestCommand;
import co.edu.uniquindio.triage.application.port.in.request.CreateRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.NextRequestIdPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class CreateRequestService implements CreateRequestUseCase {

    private final NextRequestIdPort nextRequestIdPort;
    private final LoadRequestTypePort loadRequestTypePort;
    private final LoadOriginChannelPort loadOriginChannelPort;
    private final LoadUserAuthPort loadUserAuthPort;
    private final SaveRequestPort saveRequestPort;

    public CreateRequestService(NextRequestIdPort nextRequestIdPort,
                                LoadRequestTypePort loadRequestTypePort,
                                LoadOriginChannelPort loadOriginChannelPort,
                                LoadUserAuthPort loadUserAuthPort,
                                SaveRequestPort saveRequestPort) {
        this.nextRequestIdPort = Objects.requireNonNull(nextRequestIdPort, "El nextRequestIdPort no puede ser null");
        this.loadRequestTypePort = Objects.requireNonNull(loadRequestTypePort, "El loadRequestTypePort no puede ser null");
        this.loadOriginChannelPort = Objects.requireNonNull(loadOriginChannelPort, "El loadOriginChannelPort no puede ser null");
        this.loadUserAuthPort = Objects.requireNonNull(loadUserAuthPort, "El loadUserAuthPort no puede ser null");
        this.saveRequestPort = Objects.requireNonNull(saveRequestPort, "El saveRequestPort no puede ser null");
    }

    @Override
    public RequestSummary execute(CreateRequestCommand command, AuthenticatedActor actor) {
        Objects.requireNonNull(command, "El command no puede ser null");
        Objects.requireNonNull(actor, "El actor no puede ser null");

        validateIntakeRole(actor);

        var requester = loadUserAuthPort.loadById(actor.userId())
                .orElseThrow(() -> new EntityNotFoundException("User", "id", actor.userId().value()));
        requester.ensureActive();

        var requestType = loadRequestTypePort.loadById(command.requestTypeId())
                .filter(type -> type.isActive())
                .orElseThrow(() -> new IllegalArgumentException("El tipo de solicitud no existe o está inactivo"));
        var originChannel = loadOriginChannelPort.loadById(command.originChannelId())
                .filter(channel -> channel.isActive())
                .orElseThrow(() -> new IllegalArgumentException("El canal de origen no existe o está inactivo"));

        var request = new AcademicRequest(
                nextRequestIdPort.nextId(),
                command.description(),
                requester.getId().orElseThrow(),
                command.originChannelId(),
                command.requestTypeId(),
                command.deadline(),
                false,
                LocalDateTime.now()
        );

        saveRequestPort.save(request);
        return new RequestSummary(request, requestType, originChannel, requester, Optional.empty());
    }

    private void validateIntakeRole(AuthenticatedActor actor) {
        switch (actor.role()) {
            case STUDENT, STAFF -> {
            }
            case ADMIN -> throw new UnauthorizedOperationException(Role.ADMIN, "create request");
        }
    }
}
