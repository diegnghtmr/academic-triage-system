package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper;

import co.edu.uniquindio.triage.application.port.in.ai.AiClassificationSuggestion;
import co.edu.uniquindio.triage.application.port.in.ai.AiGeneratedSummary;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.ai.AiClassificationResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.ai.AiSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class AiRestMapper {

    public AiClassificationResponse toResponse(AiClassificationSuggestion suggestion) {
        return new AiClassificationResponse(
            suggestion.requestTypeName(),
            suggestion.suggestedRequestTypeId().map(RequestTypeId::value).orElse(null),
            suggestion.suggestedPriority(),
            suggestion.confidence(),
            suggestion.reasoning()
        );
    }

    public AiSummaryResponse toResponse(AiGeneratedSummary summary) {
        return new AiSummaryResponse(
            summary.requestId().value(),
            summary.summary(),
            summary.generatedAt()
        );
    }
}
