package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.CreateRequestCommand;
import co.edu.uniquindio.triage.application.port.in.request.CreateRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;
import co.edu.uniquindio.triage.application.port.out.persistence.CreateRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.NewAcademicRequest;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class CreateRequestService implements CreateRequestUseCase {

    private final CreateRequestPort createRequestPort;
    private final LoadRequestTypePort loadRequestTypePort;
    private final LoadOriginChannelPort loadOriginChannelPort;
    private final LoadUserAuthPort loadUserAuthPort;

    public CreateRequestService(CreateRequestPort createRequestPort,
                                LoadRequestTypePort loadRequestTypePort,
                                LoadOriginChannelPort loadOriginChannelPort,
                                LoadUserAuthPort loadUserAuthPort) {
        this.createRequestPort = Objects.requireNonNull(createRequestPort, "El createRequestPort no puede ser null");
        this.loadRequestTypePort = Objects.requireNonNull(loadRequestTypePort, "El loadRequestTypePort no puede ser null");
        this.loadOriginChannelPort = Objects.requireNonNull(loadOriginChannelPort, "El loadOriginChannelPort no puede ser null");
        this.loadUserAuthPort = Objects.requireNonNull(loadUserAuthPort, "El loadUserAuthPort no puede ser null");
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

        var request = createRequestPort.create(new NewAcademicRequest(
                command.description(),
                requester.getId().orElseThrow(),
                command.originChannelId(),
                command.requestTypeId(),
                command.deadline(),
                false,
                LocalDateTime.now()
        ));
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
