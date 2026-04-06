package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule;

import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.RequestTypeResponse;

public record BusinessRuleResponse(
        Long id,
        String name,
        String description,
        ConditionType conditionType,
        String conditionValue,
        Priority resultingPriority,
        RequestTypeResponse requestType,
        boolean active
) {
}
