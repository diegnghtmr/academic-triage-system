package co.edu.uniquindio.triage.application.port.in.command.catalog;

import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;

import java.util.Objects;

public record UpdateOriginChannelCommand(
        OriginChannelId originChannelId,
        String name
) {

    private static final int MAX_NAME_LENGTH = 100;

    public UpdateOriginChannelCommand {
        Objects.requireNonNull(originChannelId, "El originChannelId no puede ser null");
        name = normalizeRequiredName(name);
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
}
