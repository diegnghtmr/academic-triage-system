package co.edu.uniquindio.triage.application.port.in.command.request;

import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.util.Objects;

public record AssignRequestCommand(
        RequestId requestId,
        UserId assignedToUserId,
        String observations
) {

    private static final int MAX_OBSERVATIONS_LENGTH = 1000;

    public AssignRequestCommand {
        Objects.requireNonNull(requestId, "El requestId no puede ser null");
        Objects.requireNonNull(assignedToUserId, "El assignedToUserId no puede ser null");
        observations = normalizeOptionalObservations(observations);
    }

    private static String normalizeOptionalObservations(String observations) {
        if (observations == null) {
            return null;
        }

        var trimmed = observations.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_OBSERVATIONS_LENGTH) {
            throw new IllegalArgumentException("Las observaciones no pueden tener más de 1000 caracteres");
        }
        return trimmed;
    }
}
