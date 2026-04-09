package co.edu.uniquindio.triage.domain.service;

import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.BusinessRule;

import java.util.List;
import java.util.Objects;

/**
 * Resultado de evaluar reglas activas contra una solicitud: prioridad sugerida y reglas que aplicaron.
 */
public record PriorityEvaluationResult(Priority suggestedPriority, List<BusinessRule> matchedRules) {

    public PriorityEvaluationResult {
        suggestedPriority = Objects.requireNonNull(suggestedPriority, "suggestedPriority no puede ser null");
        matchedRules = List.copyOf(matchedRules);
    }
}
