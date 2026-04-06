package co.edu.uniquindio.triage.application.port.in.ai;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.ai.SuggestClassificationCommand;

public interface SuggestClassificationUseCase {
    AiClassificationSuggestion execute(SuggestClassificationCommand command, AuthenticatedActor actor);
}
