package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.command.request.AddInternalNoteCommand;

public interface AddInternalNoteUseCase {
    void addInternalNote(AddInternalNoteCommand command);
}
