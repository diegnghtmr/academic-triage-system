package co.edu.uniquindio.triage.domain.exception;

import co.edu.uniquindio.triage.domain.enums.RoleEnum;

public class UnauthorizedOperationException extends DomainException {
    public UnauthorizedOperationException(RoleEnum role, String operation) {
        super(String.format("El rol %s no tiene permisos para realizar la operación: %s", role, operation));
    }

    public UnauthorizedOperationException(String message) {
        super(message);
    }
}
