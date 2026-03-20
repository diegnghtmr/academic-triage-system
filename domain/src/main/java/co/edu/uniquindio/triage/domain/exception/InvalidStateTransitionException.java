package co.edu.uniquindio.triage.domain.exception;

import co.edu.uniquindio.triage.domain.enums.RequestStatusEnum;

public class InvalidStateTransitionException extends DomainException {
    public InvalidStateTransitionException(RequestStatusEnum from, RequestStatusEnum to) {
        super(String.format("Transición de estado inválida: de %s a %s", from, to));
    }

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
