package co.edu.uniquindio.triage.domain.exception;

import co.edu.uniquindio.triage.domain.enums.Role;

public class UnauthorizedOperationException extends DomainException {

    private final Role role;
    private final String operation;

    public UnauthorizedOperationException(Role role, String operation) {
        super(String.format("El rol %s no tiene permisos para realizar la operación: %s", role, operation));
        this.role = role;
        this.operation = operation;
    }

public Role getRole() {
        return role;
    }

    public String getOperation() {
        return operation;
    }
}
