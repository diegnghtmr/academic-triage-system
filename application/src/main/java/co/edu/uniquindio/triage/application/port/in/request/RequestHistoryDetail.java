package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.domain.model.RequestHistory;
import co.edu.uniquindio.triage.domain.model.User;

import java.util.Objects;

public record RequestHistoryDetail(RequestHistory historyEntry, User performedBy) {

    public RequestHistoryDetail {
        Objects.requireNonNull(historyEntry, "El historial no puede ser null");
        Objects.requireNonNull(performedBy, "El usuario que realizó la acción no puede ser null");
    }
}
