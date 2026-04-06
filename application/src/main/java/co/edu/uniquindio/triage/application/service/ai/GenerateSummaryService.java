package co.edu.uniquindio.triage.application.service.ai;

import co.edu.uniquindio.triage.application.port.in.ai.AiGeneratedSummary;
import co.edu.uniquindio.triage.application.port.in.ai.GenerateSummaryUseCase;
import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.ai.GenerateSummaryQueryModel;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;

import java.util.Objects;

public class GenerateSummaryService implements GenerateSummaryUseCase {

    private final AiAssistantPort aiAssistantPort;
    private final LoadRequestPort loadRequestPort;
    private final AiAuthorizationSupport authSupport;

    public GenerateSummaryService(AiAssistantPort aiAssistantPort,
                                  LoadRequestPort loadRequestPort,
                                  AiAuthorizationSupport authSupport) {
        this.aiAssistantPort = Objects.requireNonNull(aiAssistantPort, "aiAssistantPort must not be null");
        this.loadRequestPort = Objects.requireNonNull(loadRequestPort, "loadRequestPort must not be null");
        this.authSupport = Objects.requireNonNull(authSupport, "authSupport must not be null");
    }

    @Override
    public AiGeneratedSummary execute(GenerateSummaryQueryModel query, AuthenticatedActor actor) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        authSupport.ensureStaffOrAdmin(actor);

        RequestDetail detail = loadRequestPort.loadDetailById(query.requestId())
            .orElseThrow(() -> new RequestNotFoundException(query.requestId()));

        AiGeneratedSummary summary = aiAssistantPort.generateSummary(detail);
        return new AiGeneratedSummary(query.requestId(), summary.summary(), summary.generatedAt());
    }
}
