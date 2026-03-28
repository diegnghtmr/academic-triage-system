package co.edu.uniquindio.triage.application.port.in.command.request;

import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.Objects;

public record CancelRequestCommand(
        RequestId requestId,
        String cancellationReason
) {

    private static final int MIN_CANCELLATION_REASON_LENGTH = 5;
    private static final int MAX_CANCELLATION_REASON_LENGTH = 2000;

    public CancelRequestCommand {
        Objects.requireNonNull(requestId, "El requestId no puede ser null");
        cancellationReason = validateCancellationReason(cancellationReason);
    }

    private static String validateCancellationReason(String cancellationReason) {
        if (cancellationReason == null || cancellationReason.isBlank()) {
            throw new IllegalArgumentException("La razón de cancelación no puede ser null o vacía");
        }

        var trimmed = cancellationReason.trim();
        if (trimmed.length() < MIN_CANCELLATION_REASON_LENGTH || trimmed.length() > MAX_CANCELLATION_REASON_LENGTH) {
            throw new IllegalArgumentException("La razón de cancelación debe tener entre 5 y 2000 caracteres");
        }
        return trimmed;
    }
}
