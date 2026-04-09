package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.domain.enums.Priority;

import java.util.List;
import java.util.Objects;

public record PrioritySuggestionResult(Priority suggestedPriority, List<MatchedBusinessRuleSummary> matchedRules) {

    public PrioritySuggestionResult {
        Objects.requireNonNull(suggestedPriority, "suggestedPriority no puede ser null");
        matchedRules = List.copyOf(matchedRules);
    }
}
