package co.edu.uniquindio.triage.application.port.in.command.businessrule;

import co.edu.uniquindio.triage.domain.enums.ConditionType;

public record ListBusinessRulesQuery(
        Boolean active,
        ConditionType conditionType
) {
}
