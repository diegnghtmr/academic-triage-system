package co.edu.uniquindio.triage.domain.event;

import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Priority;
import java.time.Instant;

public sealed interface RequestEvent permits
        RequestEvent.Registered,
        RequestEvent.Classified,
        RequestEvent.Prioritized,
        RequestEvent.Assigned,
        RequestEvent.Attended,
        RequestEvent.Closed,
        RequestEvent.Cancelled,
        RequestEvent.Rejected {

    RequestId requestId();
    Instant occurredAt();

    record Registered(RequestId requestId, UserId applicantId, Instant occurredAt)
            implements RequestEvent {}
    record Classified(RequestId requestId, String requestTypeName, Instant occurredAt)
            implements RequestEvent {}
    record Prioritized(RequestId requestId, Priority priority, String justification, Instant occurredAt)
            implements RequestEvent {}
    record Assigned(RequestId requestId, UserId responsibleId, Instant occurredAt)
            implements RequestEvent {}
    record Attended(RequestId requestId, String observation, Instant occurredAt)
            implements RequestEvent {}
    record Closed(RequestId requestId, String closingObservation, Instant occurredAt)
            implements RequestEvent {}
    record Cancelled(RequestId requestId, String reason, Instant occurredAt)
            implements RequestEvent {}
    record Rejected(RequestId requestId, String reason, Instant occurredAt)
            implements RequestEvent {}
}
