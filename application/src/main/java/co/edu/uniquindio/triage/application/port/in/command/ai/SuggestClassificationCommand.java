package co.edu.uniquindio.triage.application.port.in.command.ai;

import java.util.Objects;

public record SuggestClassificationCommand(String description) {

    private static final int MIN_LEN = 10;
    private static final int MAX_LEN = 2000;

    public SuggestClassificationCommand(String description) {
        Objects.requireNonNull(description, "La descripción no puede ser null");
        var normalized = description.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("La descripción no puede ser null o vacía");
        }
        if (normalized.length() < MIN_LEN || normalized.length() > MAX_LEN) {
            throw new IllegalArgumentException(
                    "La descripción debe tener entre " + MIN_LEN + " y " + MAX_LEN + " caracteres");
        }
        this.description = normalized;
    }
}
