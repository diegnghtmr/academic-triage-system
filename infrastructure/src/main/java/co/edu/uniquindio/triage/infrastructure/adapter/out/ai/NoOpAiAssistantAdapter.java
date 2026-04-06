package co.edu.uniquindio.triage.infrastructure.adapter.out.ai;

import co.edu.uniquindio.triage.application.port.in.ai.AiClassificationSuggestion;
import co.edu.uniquindio.triage.application.port.in.ai.AiGeneratedSummary;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.domain.exception.AiServiceUnavailableException;
import co.edu.uniquindio.triage.domain.model.RequestType;

import java.util.List;

public class NoOpAiAssistantAdapter implements AiAssistantPort {

    private final String reason;

    public NoOpAiAssistantAdapter(String reason) {
        this.reason = reason;
    }

    @Override
    public AiClassificationSuggestion suggestClassification(String description, List<RequestType> activeRequestTypes) {
        throw new AiServiceUnavailableException("El servicio de IA no está disponible: " + reason);
    }

    @Override
    public AiGeneratedSummary generateSummary(RequestDetail requestDetail) {
        throw new AiServiceUnavailableException("El servicio de IA no está disponible: " + reason);
    }
}
