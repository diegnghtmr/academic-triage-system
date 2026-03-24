package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.GetRequestDetailQueryModel;
import co.edu.uniquindio.triage.application.port.in.request.GetRequestDetailQuery;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;

import java.util.Objects;

public class GetRequestDetailService implements GetRequestDetailQuery {

    private final LoadRequestPort loadRequestPort;

    public GetRequestDetailService(LoadRequestPort loadRequestPort) {
        this.loadRequestPort = Objects.requireNonNull(loadRequestPort, "El loadRequestPort no puede ser null");
    }

    @Override
    public RequestDetail execute(GetRequestDetailQueryModel query, AuthenticatedActor actor) {
        Objects.requireNonNull(query, "El query no puede ser null");
        Objects.requireNonNull(actor, "El actor no puede ser null");

        var detail = loadRequestPort.loadDetailById(query.requestId())
                .orElseThrow(() -> new RequestNotFoundException(query.requestId()));

        if (actor.role() == Role.STUDENT && !detail.request().getApplicantId().equals(actor.userId())) {
            throw new UnauthorizedOperationException(actor.role(), "get request detail");
        }

        return detail;
    }
}
