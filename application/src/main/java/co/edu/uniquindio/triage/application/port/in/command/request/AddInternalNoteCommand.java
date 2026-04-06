package co.edu.uniquindio.triage.application.port.in.command.request;

import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.util.Objects;

public record AddInternalNoteCommand(
        RequestId requestId,
        String note,
        UserId performedById
) {
    public AddInternalNoteCommand {
        Objects.requireNonNull(requestId, "El requestId no puede ser null");
        Objects.requireNonNull(performedById, "El performedById no puede ser null");
        validateNote(note);
    }

    private static void validateNote(String note) {
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("La nota no puede ser null o vacía");
        }
        if (note.trim().length() > 2000) {
            throw new IllegalArgumentException("La nota no puede tener más de 2000 caracteres");
        }
    }
}
