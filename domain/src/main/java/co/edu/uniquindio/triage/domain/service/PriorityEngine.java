package co.edu.uniquindio.triage.domain.service;

import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.BusinessRule;

import java.util.ArrayList;
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
     */
    public Priority evaluate(AcademicRequest request, List<BusinessRule> activeRules) {
        return evaluateWithDetails(request, activeRules).suggestedPriority();
    }

    /**
     * Igual que {@link #evaluate} pero incluye la lista de reglas activas que aplicaron (sin mutar la solicitud).
     */
    public PriorityEvaluationResult evaluateWithDetails(AcademicRequest request, List<BusinessRule> activeRules) {
        Objects.requireNonNull(request, "La solicitud no puede ser null");
        Objects.requireNonNull(activeRules, "La lista de reglas no puede ser null");

        var matched = new ArrayList<BusinessRule>();
        for (BusinessRule rule : activeRules) {
            if (rule.matches(request)) {
                matched.add(rule);
            }
        }

        Optional<Priority> highestMatch = matched.stream()
                .map(BusinessRule::getResultingPriority)
                .max(PRIORITY_COMPARATOR);

        var suggested = highestMatch.orElse(Priority.LOW);
        return new PriorityEvaluationResult(suggested, List.copyOf(matched));
    }

    private static int priorityWeight(Priority priority) {
        return switch (priority) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
}
