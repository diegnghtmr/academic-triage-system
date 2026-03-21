package co.edu.uniquindio.triage.domain.model.id;

public record BusinessRuleId(Long value) {
    public BusinessRuleId {
        if (value == null) {
            throw new IllegalArgumentException("BusinessRuleId no puede ser null");
        }
        if (value <= 0) {
            throw new IllegalArgumentException("BusinessRuleId debe ser un valor positivo");
        }
    }

    public static BusinessRuleId of(Long value) {
        return new BusinessRuleId(value);
    }
}
