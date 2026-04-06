package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request;

import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user.UserResponse;

import java.time.LocalDateTime;

public record RequestHistoryResponse(
        Long id,
        HistoryAction action,
        String observations,
        LocalDateTime timestamp,
        UserResponse performedBy
) {
}
