package co.edu.uniquindio.triage.domain.exception;

public class BusinessRuleViolationException extends DomainException {

    private final String ruleDescription;

    public BusinessRuleViolationException(String ruleDescription) {
        super(String.format("Violación de regla de negocio: %s", ruleDescription));
        this.ruleDescription = ruleDescription;
    }

    public String getRuleDescription() {
        return ruleDescription;
    }
}
