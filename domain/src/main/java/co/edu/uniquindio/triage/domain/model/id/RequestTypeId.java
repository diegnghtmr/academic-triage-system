package co.edu.uniquindio.triage.domain.model.id;

public record RequestTypeId(Long value) {
    public RequestTypeId {
        if (value == null) {
            throw new IllegalArgumentException("RequestTypeId no puede ser null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("RequestTypeId debe ser un valor positivo");
        }
    }

    public static RequestTypeId of(Long value) {
        return new RequestTypeId(value);
    }
}
