package co.edu.uniquindio.triage.application.port.in.command.request;

import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.Objects;

public record AttendRequestCommand(
        RequestId requestId,
        String observations
) {

    private static final int MIN_OBSERVATIONS_LENGTH = 5;
    private static final int MAX_OBSERVATIONS_LENGTH = 2000;

    public AttendRequestCommand {
        Objects.requireNonNull(requestId, "El requestId no puede ser null");
        observations = validateObservations(observations);
    }

    private static String validateObservations(String observations) {
        if (observations == null || observations.isBlank()) {
            throw new IllegalArgumentException("La observación no puede ser null o vacía");
        }

        var trimmed = observations.trim();
        if (trimmed.length() < MIN_OBSERVATIONS_LENGTH || trimmed.length() > MAX_OBSERVATIONS_LENGTH) {
            throw new IllegalArgumentException("La observación debe tener entre 5 y 2000 caracteres");
        }
        return trimmed;
    }
}
