package co.edu.uniquindio.triage.application.port.out.ai;

import co.edu.uniquindio.triage.application.port.in.ai.AiGeneratedSummary;
import co.edu.uniquindio.triage.domain.model.id.RequestId;

public interface SaveAiSummaryCachePort {
    void save(RequestId requestId, long requestVersion, AiGeneratedSummary summary);
}
