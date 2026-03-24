package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.User;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record RequestDetail(
        AcademicRequest request,
        RequestType requestType,
        OriginChannel originChannel,
        User requester,
        Optional<User> assignedTo,
        List<RequestHistoryDetail> history
) {

    public RequestDetail {
        Objects.requireNonNull(request, "La solicitud no puede ser null");
        Objects.requireNonNull(requestType, "El tipo de solicitud no puede ser null");
        Objects.requireNonNull(originChannel, "El canal de origen no puede ser null");
        Objects.requireNonNull(requester, "El solicitante no puede ser null");
        assignedTo = assignedTo == null ? Optional.empty() : assignedTo;
        history = history == null ? List.of() : List.copyOf(history);
    }
}
