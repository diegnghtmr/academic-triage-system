package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request;

import co.edu.uniquindio.triage.domain.enums.Priority;

public record MatchedRuleSuggestionResponse(
        Long ruleId,
        String name,
        Priority resultingPriority
) {
}
