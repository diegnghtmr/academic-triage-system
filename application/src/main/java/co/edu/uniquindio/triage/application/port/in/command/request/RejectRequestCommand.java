package co.edu.uniquindio.triage.application.port.in.command.request;

import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.Objects;

public record RejectRequestCommand(
        RequestId requestId,
        String rejectionReason
) {

    private static final int MIN_REJECTION_REASON_LENGTH = 5;
    private static final int MAX_REJECTION_REASON_LENGTH = 2000;

    public RejectRequestCommand {
        Objects.requireNonNull(requestId, "El requestId no puede ser null");
        rejectionReason = validateRejectionReason(rejectionReason);
    }

    private static String validateRejectionReason(String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException("La razón de rechazo no puede ser null o vacía");
        }

        var trimmed = rejectionReason.trim();
        if (trimmed.length() < MIN_REJECTION_REASON_LENGTH || trimmed.length() > MAX_REJECTION_REASON_LENGTH) {
            throw new IllegalArgumentException("La razón de rechazo debe tener entre 5 y 2000 caracteres");
        }
        return trimmed;
    }
}
