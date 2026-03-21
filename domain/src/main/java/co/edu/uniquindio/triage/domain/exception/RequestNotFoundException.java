package co.edu.uniquindio.triage.domain.exception;

import co.edu.uniquindio.triage.domain.model.id.RequestId;

public class RequestNotFoundException extends DomainException {

    private final RequestId requestId;

    public RequestNotFoundException(RequestId requestId) {
        super(String.format("No se encontró la solicitud con id: %s", requestId));
        this.requestId = requestId;
    }

    public RequestId getRequestId() {
        return requestId;
    }
}
