package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.model.id.RequestHistoryId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

public class RequestHistory {
    private final RequestHistoryId id;
    private final HistoryAction action;
    private final String observations;
    private final LocalDateTime timestamp;
    private final RequestId requestId;
    private final UserId performedById;
    private final UserId responsibleId;

    public RequestHistory(RequestHistoryId id, HistoryAction action, String observations,
                          LocalDateTime timestamp, RequestId requestId, UserId performedById) {
        this(id, action, observations, timestamp, requestId, performedById, null);
    }

    public RequestHistory(RequestHistoryId id, HistoryAction action, String observations,
                          LocalDateTime timestamp, RequestId requestId, UserId performedById,
                          UserId responsibleId) {
        this.id = id;
        this.action = Objects.requireNonNull(action, "El action no puede ser null");
        this.observations = validateObservations(observations);
        this.timestamp = Objects.requireNonNull(timestamp, "El timestamp no puede ser null");
        this.requestId = Objects.requireNonNull(requestId, "El requestId no puede ser null");
        this.performedById = Objects.requireNonNull(performedById, "El performedById no puede ser null");
        this.responsibleId = responsibleId;
    }

    private String validateObservations(String observations) {
        if (observations == null) {
            return null;
        }
        var trimmed = observations.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 2000) {
            throw new IllegalArgumentException("Las observaciones no pueden tener más de 2000 caracteres");
        }
        return trimmed;
    }

    public RequestHistoryId getId() {
        return id;
    }

    public HistoryAction getAction() {
        return action;
    }

    public String getObservations() {
        return observations;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public RequestId getRequestId() {
        return requestId;
    }

    public UserId getPerformedById() {
        return performedById;
    }

    public UserId getResponsibleId() {
        return responsibleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestHistory that = (RequestHistory) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return action == that.action
                && Objects.equals(observations, that.observations)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(requestId, that.requestId)
                && Objects.equals(performedById, that.performedById)
                && Objects.equals(responsibleId, that.responsibleId);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(action, observations, timestamp, requestId, performedById, responsibleId);
    }

    @Override
    public String toString() {
        return "RequestHistory{" +
                "id=" + id +
                ", action=" + action +
                ", timestamp=" + timestamp +
                ", requestId=" + requestId +
                ", performedById=" + performedById +
                ", responsibleId=" + responsibleId +
                '}';
    }
}
