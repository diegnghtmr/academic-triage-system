package co.edu.uniquindio.triage.application.port.in.businessrule;

import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.RequestType;

import java.util.Objects;

/**
 * Vista de regla con tipo de solicitud hidratado cuando existe en catálogo; si no, {@code requestType} es null.
 */
public record BusinessRuleView(BusinessRule rule, RequestType requestType) {

    public BusinessRuleView {
        Objects.requireNonNull(rule, "rule no puede ser null");
    }
}
