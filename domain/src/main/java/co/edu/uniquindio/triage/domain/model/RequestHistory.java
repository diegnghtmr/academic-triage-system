package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.model.id.RequestHistoryId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.time.LocalDateTime;
import java.util.Objects;

public class RequestHistory {
    private RequestHistoryId id;
    private String action;
    private String observations;
    private LocalDateTime timestamp;
    private RequestId requestId;
    private UserId performedById;

    public RequestHistory(RequestHistoryId id, String action, String observations,
                          RequestId requestId, UserId performedById) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
        this.action = validateAction(action);
        this.observations = validateObservations(observations);
        this.timestamp = LocalDateTime.now();
        this.requestId = Objects.requireNonNull(requestId, "El requestId no puede ser null");
        this.performedById = Objects.requireNonNull(performedById, "El performedById no puede ser null");
    }

    private String validateAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("La acción no puede ser null o vacía");
        }
        String trimmed = action.trim();
        if (trimmed.length() > 50) {
            throw new IllegalArgumentException("La acción no puede tener más de 50 caracteres");
        }
        return trimmed;
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

    public void setId(RequestHistoryId id) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
    }

    public String getAction() {
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
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                ", requestId=" + requestId +
                '}';
    }
}
