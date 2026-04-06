package co.edu.uniquindio.triage.application.port.in.ai;

import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.time.LocalDateTime;

public record AiGeneratedSummary(
    RequestId requestId,
    String summary,
    LocalDateTime generatedAt
) {
}
