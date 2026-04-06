package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper;

import co.edu.uniquindio.triage.application.port.in.command.request.AddInternalNoteCommand;
import co.edu.uniquindio.triage.application.port.in.request.RequestHistoryDetail;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.AddInternalNoteRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.RequestHistoryResponse;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class RequestHistoryRestMapper {

    private final UserRestMapper userRestMapper = new UserRestMapper();

    public AddInternalNoteCommand toCommand(Long requestId, UserId performedById, AddInternalNoteRequest request) {
        Objects.requireNonNull(requestId, "El requestId no puede ser null");
        Objects.requireNonNull(performedById, "El performedById no puede ser null");
        Objects.requireNonNull(request, "El request no puede ser null");
        return new AddInternalNoteCommand(
                new RequestId(requestId),
                request.observations(),
                performedById
        );
    }

    public RequestHistoryResponse toResponse(RequestHistoryDetail detail) {
        Objects.requireNonNull(detail, "El detalle del historial no puede ser null");
        var entry = detail.historyEntry();
        return new RequestHistoryResponse(
                entry.getId() != null ? entry.getId().value() : null,
                entry.getAction(),
                entry.getObservations(),
                entry.getTimestamp(),
                userRestMapper.toResponse(detail.performedBy())
        );
    }
}
