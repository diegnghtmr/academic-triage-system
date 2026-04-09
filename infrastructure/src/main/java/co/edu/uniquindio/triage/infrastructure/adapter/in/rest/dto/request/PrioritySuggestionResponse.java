package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request;

import co.edu.uniquindio.triage.domain.enums.Priority;

import java.util.List;

public record PrioritySuggestionResponse(
        Priority suggestedPriority,
        List<MatchedRuleSuggestionResponse> matchedRules
) {
}
