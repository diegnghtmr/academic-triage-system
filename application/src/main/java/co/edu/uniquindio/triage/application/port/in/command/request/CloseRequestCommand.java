package co.edu.uniquindio.triage.application.port.in.command.request;

import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.Objects;

public record CloseRequestCommand(
        RequestId requestId,
        String closingObservation
) {

    private static final int MIN_CLOSING_OBSERVATION_LENGTH = 5;
    private static final int MAX_CLOSING_OBSERVATION_LENGTH = 2000;

    public CloseRequestCommand {
        Objects.requireNonNull(requestId, "El requestId no puede ser null");
        closingObservation = validateClosingObservation(closingObservation);
    }

    private static String validateClosingObservation(String closingObservation) {
        if (closingObservation == null || closingObservation.isBlank()) {
            throw new IllegalArgumentException("La observación de cierre no puede ser null o vacía");
        }

        var trimmed = closingObservation.trim();
        if (trimmed.length() < MIN_CLOSING_OBSERVATION_LENGTH || trimmed.length() > MAX_CLOSING_OBSERVATION_LENGTH) {
            throw new IllegalArgumentException("La observación de cierre debe tener entre 5 y 2000 caracteres");
        }
        return trimmed;
    }
}
