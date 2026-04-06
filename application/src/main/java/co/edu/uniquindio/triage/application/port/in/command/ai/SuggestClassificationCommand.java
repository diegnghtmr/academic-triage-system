package co.edu.uniquindio.triage.application.port.in.command.ai;

import java.util.Objects;

public record SuggestClassificationCommand(String description) {
    public SuggestClassificationCommand {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("La descripción no puede ser null o vacía");
        }
    }
}
