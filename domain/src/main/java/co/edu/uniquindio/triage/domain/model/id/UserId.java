package co.edu.uniquindio.triage.domain.model.id;

public record UserId(Long value) {
    public UserId {
        if (value == null) {
            throw new IllegalArgumentException("UserId no puede ser null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("UserId debe ser un valor positivo");
        }
    }

    public static UserId of(Long value) {
        return new UserId(value);
    }
}
