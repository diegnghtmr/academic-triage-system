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

    public RequestHistory(RequestHistoryId id, HistoryAction action, String observations,
                          LocalDateTime timestamp, RequestId requestId, UserId performedById) {
        this.id = id;
        this.action = Objects.requireNonNull(action, "El action no puede ser null");
        this.observations = validateObservations(observations);
        this.timestamp = Objects.requireNonNull(timestamp, "El timestamp no puede ser null");
        this.requestId = Objects.requireNonNull(requestId, "El requestId no puede ser null");
        this.performedById = Objects.requireNonNull(performedById, "El performedById no puede ser null");
    }

    private String validateObservations(String observations) {
        if (observations == null) {
            return null;
        }
        if (observations.length() > 2000) {
            throw new IllegalArgumentException("Las observaciones no pueden tener más de 2000 caracteres");
        }
        return observations.trim();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestHistory that = (RequestHistory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RequestHistory{" +
                "id=" + id +
                ", action=" + action +
                ", timestamp=" + timestamp +
                ", requestId=" + requestId +
                '}';
    }
}
