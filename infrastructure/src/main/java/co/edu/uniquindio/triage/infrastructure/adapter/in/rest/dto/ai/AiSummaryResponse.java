package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.ai;

import java.time.LocalDateTime;

public record AiSummaryResponse(
    Long requestId,
    String summary,
    LocalDateTime generatedAt
) {
}
