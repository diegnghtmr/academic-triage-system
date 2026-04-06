package co.edu.uniquindio.triage.application.port.in.ai;

import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.util.Optional;

public record AiClassificationSuggestion(
    String requestTypeName,
    Optional<RequestTypeId> suggestedRequestTypeId,
    Priority suggestedPriority,
    double confidence,
    String reasoning
) {
}
