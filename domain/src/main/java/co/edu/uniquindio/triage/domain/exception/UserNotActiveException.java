package co.edu.uniquindio.triage.domain.exception;

import co.edu.uniquindio.triage.domain.model.id.UserId;

public class UserNotActiveException extends DomainException {

    private final UserId userId;

    public UserNotActiveException(UserId userId) {
        super(String.format("El usuario con id %s no está activo", userId));
        this.userId = userId;
    }

    public UserId getUserId() {
        return userId;
    }
}
