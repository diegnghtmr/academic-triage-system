package co.edu.uniquindio.triage.domain.exception;

import co.edu.uniquindio.triage.domain.enums.RequestStatus;

public class InvalidStateTransitionException extends DomainException {

    private final RequestStatus fromStatus;
    private final RequestStatus toStatus;

    public InvalidStateTransitionException(RequestStatus fromStatus, RequestStatus toStatus) {
        super(String.format("Transición de estado inválida: de %s a %s", fromStatus, toStatus));
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
    }

public RequestStatus getFromStatus() {
        return fromStatus;
    }

    public RequestStatus getToStatus() {
        return toStatus;
    }
}
