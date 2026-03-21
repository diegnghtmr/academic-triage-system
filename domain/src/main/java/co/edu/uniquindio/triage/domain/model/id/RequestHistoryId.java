package co.edu.uniquindio.triage.domain.model.id;

public record RequestHistoryId(Long value) {
    public RequestHistoryId {
        if (value == null) {
            throw new IllegalArgumentException("RequestHistoryId no puede ser null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("RequestHistoryId debe ser un valor positivo");
        }
    }

    public static RequestHistoryId of(Long value) {
        return new RequestHistoryId(value);
    }
}
