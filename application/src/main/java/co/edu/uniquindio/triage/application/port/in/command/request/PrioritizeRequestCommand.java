package co.edu.uniquindio.triage.application.port.in.command.request;

import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.Objects;

public record PrioritizeRequestCommand(
        RequestId requestId,
        Priority priority,
        String justification
) {

    private static final int MIN_JUSTIFICATION_LENGTH = 5;
    private static final int MAX_JUSTIFICATION_LENGTH = 1000;

    public PrioritizeRequestCommand {
        Objects.requireNonNull(requestId, "El requestId no puede ser null");
        Objects.requireNonNull(priority, "La prioridad no puede ser null");
        justification = validateJustification(justification);
    }

    private static String validateJustification(String justification) {
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException("La justificación no puede ser null o vacía");
        }

        var trimmed = justification.trim();
        if (trimmed.length() < MIN_JUSTIFICATION_LENGTH || trimmed.length() > MAX_JUSTIFICATION_LENGTH) {
            throw new IllegalArgumentException("La justificación debe tener entre 5 y 1000 caracteres");
        }
        return trimmed;
    }
}
