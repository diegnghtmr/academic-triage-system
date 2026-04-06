package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.ai;

import co.edu.uniquindio.triage.domain.enums.Priority;

public record AiClassificationResponse(
    String suggestedRequestType,
    Long suggestedRequestTypeId,
    Priority suggestedPriority,
    double confidence,
    String reasoning
) {
}
