package co.edu.uniquindio.triage.application.service.ai;

import co.edu.uniquindio.triage.application.port.in.ai.AiGeneratedSummary;
import co.edu.uniquindio.triage.application.port.in.ai.GenerateSummaryUseCase;
import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.ai.GenerateSummaryQueryModel;
import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.application.port.out.ai.LoadAiSummaryCachePort;
import co.edu.uniquindio.triage.application.port.out.ai.SaveAiSummaryCachePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestVersionPort;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;

import java.util.Objects;

public class GenerateSummaryService implements GenerateSummaryUseCase {

    private final AiAssistantPort aiAssistantPort;
    private final LoadRequestPort loadRequestPort;
    private final LoadRequestVersionPort loadRequestVersionPort;
    private final LoadAiSummaryCachePort loadAiSummaryCachePort;
    private final SaveAiSummaryCachePort saveAiSummaryCachePort;
    private final AiAuthorizationSupport authSupport;

    public GenerateSummaryService(AiAssistantPort aiAssistantPort,
                                  LoadRequestPort loadRequestPort,
                                  LoadRequestVersionPort loadRequestVersionPort,
                                  LoadAiSummaryCachePort loadAiSummaryCachePort,
                                  SaveAiSummaryCachePort saveAiSummaryCachePort,
                                  AiAuthorizationSupport authSupport) {
        this.aiAssistantPort = Objects.requireNonNull(aiAssistantPort, "aiAssistantPort must not be null");
        this.loadRequestPort = Objects.requireNonNull(loadRequestPort, "loadRequestPort must not be null");
        this.loadRequestVersionPort = Objects.requireNonNull(loadRequestVersionPort, "loadRequestVersionPort must not be null");
        this.loadAiSummaryCachePort = Objects.requireNonNull(loadAiSummaryCachePort, "loadAiSummaryCachePort must not be null");
        this.saveAiSummaryCachePort = Objects.requireNonNull(saveAiSummaryCachePort, "saveAiSummaryCachePort must not be null");
        this.authSupport = Objects.requireNonNull(authSupport, "authSupport must not be null");
    }

    @Override
    public AiGeneratedSummary execute(GenerateSummaryQueryModel query, AuthenticatedActor actor) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        authSupport.ensureStaffOrAdmin(actor);

        var requestId = query.requestId();
        long requestVersion = loadRequestVersionPort.findVersionById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));

        var cached = loadAiSummaryCachePort.findByRequestIdAndVersion(requestId, requestVersion);
        if (cached.isPresent()) {
            return cached.get();
        }

        var detail = loadRequestPort.loadDetailById(requestId)
                .orElseThrow(() -> new RequestNotFoundException(requestId));

        var fresh = aiAssistantPort.generateSummary(detail);
        var result = new AiGeneratedSummary(requestId, fresh.summary(), fresh.generatedAt());
        saveAiSummaryCachePort.save(requestId, requestVersion, result);
        return result;
    }
}
