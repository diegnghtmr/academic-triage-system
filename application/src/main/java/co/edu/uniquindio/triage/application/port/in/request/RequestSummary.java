package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.User;

import java.util.Objects;
import java.util.Optional;

public record RequestSummary(
        AcademicRequest request,
        RequestType requestType,
        OriginChannel originChannel,
        User requester,
        Optional<User> assignedTo
) {

    public RequestSummary {
        Objects.requireNonNull(request, "La solicitud no puede ser null");
        Objects.requireNonNull(requestType, "El tipo de solicitud no puede ser null");
        Objects.requireNonNull(originChannel, "El canal de origen no puede ser null");
        Objects.requireNonNull(requester, "El solicitante no puede ser null");
        assignedTo = assignedTo == null ? Optional.empty() : assignedTo;
    }
}
