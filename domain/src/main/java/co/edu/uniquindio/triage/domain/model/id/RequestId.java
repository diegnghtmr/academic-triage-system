package co.edu.uniquindio.triage.domain.model.id;

public record RequestId(Long value) {
    public RequestId {
        if (value == null) {
            throw new IllegalArgumentException("RequestId no puede ser null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("RequestId debe ser un valor positivo");
        }
    }

    public static RequestId of(Long value) {
        return new RequestId(value);
    }
}
