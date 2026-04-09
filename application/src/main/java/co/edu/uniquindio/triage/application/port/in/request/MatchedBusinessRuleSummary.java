package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;

import java.util.Objects;

public record MatchedBusinessRuleSummary(BusinessRuleId ruleId, String name, Priority resultingPriority) {

    public MatchedBusinessRuleSummary {
        Objects.requireNonNull(ruleId, "ruleId no puede ser null");
        Objects.requireNonNull(name, "name no puede ser null");
        Objects.requireNonNull(resultingPriority, "resultingPriority no puede ser null");
    }
}
