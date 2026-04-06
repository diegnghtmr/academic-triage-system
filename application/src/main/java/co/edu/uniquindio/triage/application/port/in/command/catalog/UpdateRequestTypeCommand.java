package co.edu.uniquindio.triage.application.port.in.command.catalog;

import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.util.Objects;

public record UpdateRequestTypeCommand(
        RequestTypeId requestTypeId,
        String name,
        String description
) {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    public UpdateRequestTypeCommand {
        Objects.requireNonNull(requestTypeId, "El requestTypeId no puede ser null");
        name = normalizeRequiredName(name);
        description = normalizeOptionalDescription(description);
    }

    private static String normalizeRequiredName(String name) {
        Objects.requireNonNull(name, "El nombre no puede ser null o vacío");
        var trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede ser null o vacío");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("El nombre no puede tener más de 100 caracteres");
        }
        return trimmed;
    }

    private static String normalizeOptionalDescription(String description) {
        if (description == null) {
            return null;
        }
        var trimmed = description.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("La descripción no puede tener más de 500 caracteres");
        }
        return trimmed;
    }
}
