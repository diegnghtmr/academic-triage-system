package co.edu.uniquindio.triage.application.port.in.ai;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.ai.GenerateSummaryQueryModel;

public interface GenerateSummaryUseCase {
    AiGeneratedSummary execute(GenerateSummaryQueryModel query, AuthenticatedActor actor);
}
