package co.edu.uniquindio.triage.domain.service;

import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.BusinessRule;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain service that evaluates active BusinessRule entities against a request
 * to determine the appropriate priority. Spans BusinessRule + AcademicRequest aggregates.
 */
public class PriorityEngine {

    private static final Comparator<Priority> PRIORITY_COMPARATOR =
            Comparator.comparingInt(PriorityEngine::priorityWeight);

    /**
     * Evaluates the given active rules against a request and returns the highest priority
     * that matches. If no rules match, returns {@link Priority#LOW} as default.
     *
     * @param request     the academic request to evaluate
     * @param activeRules the list of active business rules to check
     * @return the highest matching priority, or LOW if no rules match
     */
    public Priority evaluate(AcademicRequest request, List<BusinessRule> activeRules) {
        Objects.requireNonNull(request, "La solicitud no puede ser null");
        Objects.requireNonNull(activeRules, "La lista de reglas no puede ser null");

        Optional<Priority> highestMatch = activeRules.stream()
                .filter(rule -> rule.matches(request))
                .map(BusinessRule::getResultingPriority)
                .max(PRIORITY_COMPARATOR);

        return highestMatch.orElse(Priority.LOW);
    }

    private static int priorityWeight(Priority priority) {
        return switch (priority) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
}
