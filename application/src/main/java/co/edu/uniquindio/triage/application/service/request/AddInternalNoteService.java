package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.command.request.AddInternalNoteCommand;
import co.edu.uniquindio.triage.application.port.in.request.AddInternalNoteUseCase;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;

import java.time.LocalDateTime;
import java.util.Objects;

public class AddInternalNoteService implements AddInternalNoteUseCase {

    private final LoadRequestPort loadRequestPort;
    private final SaveRequestPort saveRequestPort;

    public AddInternalNoteService(LoadRequestPort loadRequestPort, SaveRequestPort saveRequestPort) {
        this.loadRequestPort = Objects.requireNonNull(loadRequestPort, "El loadRequestPort no puede ser null");
        this.saveRequestPort = Objects.requireNonNull(saveRequestPort, "El saveRequestPort no puede ser null");
    }

    @Override
    public void addInternalNote(AddInternalNoteCommand command) {
        Objects.requireNonNull(command, "El command no puede ser null");

        var request = loadRequestPort.loadById(command.requestId())
                .orElseThrow(() -> new RequestNotFoundException(command.requestId()));

        request.addInternalNote(command.note(), command.performedById(), LocalDateTime.now());
        
        saveRequestPort.save(request);
    }
}
