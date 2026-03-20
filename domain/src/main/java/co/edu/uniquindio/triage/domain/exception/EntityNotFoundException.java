package co.edu.uniquindio.triage.domain.exception;

public class EntityNotFoundException extends DomainException {
    public EntityNotFoundException(String entityName, String fieldName, Object fieldValue) {
        super(String.format("No se encontró la entidad %s con %s: %s", entityName, fieldName, fieldValue));
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
