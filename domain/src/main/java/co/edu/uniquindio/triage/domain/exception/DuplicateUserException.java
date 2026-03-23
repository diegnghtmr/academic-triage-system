package co.edu.uniquindio.triage.domain.exception;

public class DuplicateUserException extends DomainException {

    private final String field;
    private final String value;

    public DuplicateUserException(String field, String value) {
        super("Ya existe un usuario con " + field + " '" + value + "'");
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
