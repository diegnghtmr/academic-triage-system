package co.edu.uniquindio.triage.application.port.out.ai;

import co.edu.uniquindio.triage.application.port.in.ai.AiGeneratedSummary;
import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.Optional;

public interface LoadAiSummaryCachePort {
    Optional<AiGeneratedSummary> findByRequestIdAndVersion(RequestId requestId, long requestVersion);
}
