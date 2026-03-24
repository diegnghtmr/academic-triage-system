package co.edu.uniquindio.triage.application.port.in.command.request;

import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.time.LocalDate;
import java.util.Objects;

public record CreateRequestCommand(
        RequestTypeId requestTypeId,
        OriginChannelId originChannelId,
        String description,
        LocalDate deadline
) {

    private static final int MIN_DESCRIPTION_LENGTH = 10;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    public CreateRequestCommand {
        Objects.requireNonNull(requestTypeId, "El requestTypeId no puede ser null");
        Objects.requireNonNull(originChannelId, "El originChannelId no puede ser null");
        description = validateDescription(description);
    }

    private static String validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("La descripción no puede ser null o vacía");
        }
        var trimmed = description.trim();
        if (trimmed.length() < MIN_DESCRIPTION_LENGTH || trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("La descripción debe tener entre 10 y 2000 caracteres");
        }
        return trimmed;
    }
}
