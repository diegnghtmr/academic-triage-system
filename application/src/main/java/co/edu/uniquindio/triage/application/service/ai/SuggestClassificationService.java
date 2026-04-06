package co.edu.uniquindio.triage.application.service.ai;

import co.edu.uniquindio.triage.application.port.in.ai.AiClassificationSuggestion;
import co.edu.uniquindio.triage.application.port.in.ai.SuggestClassificationUseCase;
import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.ai.SuggestClassificationCommand;
import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SuggestClassificationService implements SuggestClassificationUseCase {

    private final AiAssistantPort aiAssistantPort;
    private final LoadRequestTypePort loadRequestTypePort;
    private final AiAuthorizationSupport authSupport;

    public SuggestClassificationService(AiAssistantPort aiAssistantPort,
                                        LoadRequestTypePort loadRequestTypePort,
                                        AiAuthorizationSupport authSupport) {
        this.aiAssistantPort = Objects.requireNonNull(aiAssistantPort, "aiAssistantPort must not be null");
        this.loadRequestTypePort = Objects.requireNonNull(loadRequestTypePort, "loadRequestTypePort must not be null");
        this.authSupport = Objects.requireNonNull(authSupport, "authSupport must not be null");
    }

    @Override
    public AiClassificationSuggestion execute(SuggestClassificationCommand command, AuthenticatedActor actor) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        authSupport.ensureStaff(actor);

        List<RequestType> activeRequestTypes = loadRequestTypePort.loadAllRequestTypes(Optional.of(true));

        AiClassificationSuggestion suggestion = aiAssistantPort.suggestClassification(command.description(), activeRequestTypes);

        Optional<RequestTypeId> suggestedId = activeRequestTypes.stream()
            .filter(rt -> rt.getName().equalsIgnoreCase(suggestion.requestTypeName()))
            .map(RequestType::getId)
            .findFirst();

        return new AiClassificationSuggestion(
            suggestion.requestTypeName(),
            suggestedId,
            suggestion.suggestedPriority(),
            suggestion.confidence(),
            suggestion.reasoning()
        );
    }
}
