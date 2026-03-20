package co.edu.uniquindio.triage.domain.model.id;

public record RequestRuleId(Long value) {
    public RequestRuleId {
        if (value == null) {
            throw new IllegalArgumentException("RequestRuleId no puede ser null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("RequestRuleId debe ser un valor positivo");
        }
    }

    public static RequestRuleId of(Long value) {
        return new RequestRuleId(value);
    }
}
