package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.command.request.AddInternalNoteCommand;
import co.edu.uniquindio.triage.application.port.in.request.AddInternalNoteUseCase;
import co.edu.uniquindio.triage.application.port.in.request.RequestHistoryDetail;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestForMutationPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;

import java.time.LocalDateTime;
import java.util.Objects;

public class AddInternalNoteService implements AddInternalNoteUseCase {

    private final LoadRequestForMutationPort loadRequestForMutationPort;
    private final LoadRequestPort loadRequestPort;
    private final SaveRequestPort saveRequestPort;

    public AddInternalNoteService(LoadRequestForMutationPort loadRequestForMutationPort,
                                  LoadRequestPort loadRequestPort,
                                  SaveRequestPort saveRequestPort) {
        this.loadRequestForMutationPort = Objects.requireNonNull(loadRequestForMutationPort, "El loadRequestForMutationPort no puede ser null");
        this.loadRequestPort = Objects.requireNonNull(loadRequestPort, "El loadRequestPort no puede ser null");
        this.saveRequestPort = Objects.requireNonNull(saveRequestPort, "El saveRequestPort no puede ser null");
    }

    @Override
    public RequestHistoryDetail addInternalNote(AddInternalNoteCommand command) {
        Objects.requireNonNull(command, "El command no puede ser null");

        var request = loadRequestForMutationPort.loadByIdForMutation(command.requestId())
                .orElseThrow(() -> new RequestNotFoundException(command.requestId()));

        request.addInternalNote(command.note(), command.performedById(), LocalDateTime.now());

        saveRequestPort.save(request);

        return loadRequestPort.loadDetailById(command.requestId())
                .map(detail -> detail.history().getLast())
                .orElseThrow(() -> new RequestNotFoundException(command.requestId()));
    }
}
