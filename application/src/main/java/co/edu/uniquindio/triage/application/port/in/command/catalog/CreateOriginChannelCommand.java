package co.edu.uniquindio.triage.application.port.in.command.catalog;

import java.util.Objects;

public record CreateOriginChannelCommand(String name) {

    private static final int MAX_NAME_LENGTH = 100;

    public CreateOriginChannelCommand {
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
