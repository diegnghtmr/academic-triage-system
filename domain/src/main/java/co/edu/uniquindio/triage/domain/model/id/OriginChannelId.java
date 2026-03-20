package co.edu.uniquindio.triage.domain.model.id;

public record OriginChannelId(Long value) {
    public OriginChannelId {
        if (value == null) {
            throw new IllegalArgumentException("OriginChannelId no puede ser null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("OriginChannelId debe ser un valor positivo");
        }
    }

    public static OriginChannelId of(Long value) {
        return new OriginChannelId(value);
    }
}
