package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request;

import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.OriginChannelResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.RequestTypeResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user.UserResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RequestDetailResponse(
        Long id,
        String description,
        LocalDateTime registrationDateTime,
        RequestStatus status,
        Priority priority,
        String priorityJustification,
        String closingObservation,
        String cancellationReason,
        String rejectionReason,
        LocalDate deadline,
        boolean aiSuggested,
        RequestTypeResponse requestType,
        OriginChannelResponse originChannel,
        UserResponse requester,
        UserResponse assignedTo,
        List<HistoryEntryResponse> history
) {
}
