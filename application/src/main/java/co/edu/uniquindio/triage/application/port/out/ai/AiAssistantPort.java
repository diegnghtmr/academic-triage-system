package co.edu.uniquindio.triage.application.port.out.ai;

import co.edu.uniquindio.triage.application.port.in.ai.AiClassificationSuggestion;
import co.edu.uniquindio.triage.application.port.in.ai.AiGeneratedSummary;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.domain.model.RequestType;

import java.util.List;

public interface AiAssistantPort {
    AiClassificationSuggestion suggestClassification(String description, List<RequestType> activeRequestTypes);
    AiGeneratedSummary generateSummary(RequestDetail requestDetail);
}
