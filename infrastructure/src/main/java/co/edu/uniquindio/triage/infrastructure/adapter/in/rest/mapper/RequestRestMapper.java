package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper;

import co.edu.uniquindio.triage.application.port.in.command.request.AddInternalNoteCommand;
import co.edu.uniquindio.triage.application.port.in.command.request.AssignRequestCommand;
import co.edu.uniquindio.triage.application.port.in.command.request.AttendRequestCommand;
import co.edu.uniquindio.triage.application.port.in.command.request.CancelRequestCommand;
import co.edu.uniquindio.triage.application.port.in.command.request.ClassifyRequestCommand;
import co.edu.uniquindio.triage.application.port.in.command.request.CloseRequestCommand;
import co.edu.uniquindio.triage.application.port.in.command.request.CreateRequestCommand;
import co.edu.uniquindio.triage.application.port.in.command.request.GetRequestDetailQueryModel;
import co.edu.uniquindio.triage.application.port.in.command.request.ListRequestsQueryModel;
import co.edu.uniquindio.triage.application.port.in.command.request.PrioritizeRequestCommand;
import co.edu.uniquindio.triage.application.port.in.command.request.RejectRequestCommand;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.in.request.RequestHistoryDetail;
import co.edu.uniquindio.triage.application.port.in.request.RequestPage;
import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.RequestHistory;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.OriginChannelResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.RequestTypeResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.AddInternalNoteRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.AssignRequestRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.AttendRequestRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.CancelRequestRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.ClassifyRequestRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.CloseRequestRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.CreateRequestRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.HistoryEntryResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.PagedRequestResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.PrioritizeRequestRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.RejectRequestRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.RequestDetailResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.RequestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

@Mapper(componentModel = "spring")
public interface RequestRestMapper {

    default CreateRequestCommand toCommand(CreateRequestRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new CreateRequestCommand(
                new RequestTypeId(request.requestTypeId()),
                new OriginChannelId(request.originChannelId()),
                request.description(),
                request.deadline()
        );
    }

    default ClassifyRequestCommand toCommand(Long requestId, ClassifyRequestRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new ClassifyRequestCommand(
                new RequestId(requestId),
                new RequestTypeId(request.requestTypeId()),
                request.observations()
        );
    }

    default PrioritizeRequestCommand toCommand(Long requestId, PrioritizeRequestRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new PrioritizeRequestCommand(
                new RequestId(requestId),
                request.priority(),
                request.justification()
        );
    }

    default AssignRequestCommand toCommand(Long requestId, AssignRequestRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new AssignRequestCommand(
                new RequestId(requestId),
                new UserId(request.assignedToUserId()),
                request.observations()
        );
    }

    default AttendRequestCommand toCommand(Long requestId, AttendRequestRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new AttendRequestCommand(
                new RequestId(requestId),
                request.observations()
        );
    }

    default CloseRequestCommand toCommand(Long requestId, CloseRequestRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new CloseRequestCommand(
                new RequestId(requestId),
                request.closingObservation()
        );
    }

    default CancelRequestCommand toCommand(Long requestId, CancelRequestRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new CancelRequestCommand(
                new RequestId(requestId),
                request.cancellationReason()
        );
    }

    default RejectRequestCommand toCommand(Long requestId, RejectRequestRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new RejectRequestCommand(
                new RequestId(requestId),
                request.rejectionReason()
        );
    }

    default AddInternalNoteCommand toCommand(Long requestId, AddInternalNoteRequest request, UserId authorId) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new AddInternalNoteCommand(
                new RequestId(requestId),
                request.observations(),
                authorId
        );
    }

    default ListRequestsQueryModel toQueryModel(Optional<RequestStatus> status,
                                                Optional<Long> requestTypeId,
                                                Optional<Priority> priority,
                                                Optional<Long> assignedToUserId,
                                                Optional<Long> requesterUserId,
                                                Optional<LocalDate> dateFrom,
                                                Optional<LocalDate> dateTo,
                                                int page,
                                                int size,
                                                String sort) {
        return new ListRequestsQueryModel(
                status,
                requestTypeId.map(RequestTypeId::new),
                priority,
                assignedToUserId.map(UserId::new),
                requesterUserId.map(UserId::new),
                dateFrom,
                dateTo,
                page,
                size,
                sort
        );
    }

    default GetRequestDetailQueryModel toDetailQuery(Long requestId) {
        return new GetRequestDetailQueryModel(new RequestId(requestId));
    }

    default RequestResponse toResponse(RequestSummary summary) {
        Objects.requireNonNull(summary, "El resumen de solicitud no puede ser null");

        var request = summary.request();
        return new RequestResponse(
                request.getId().value(),
                request.getDescription(),
                request.getRegistrationDateTime(),
                request.getStatus(),
                request.getPriority(),
                request.getPriorityJustification(),
                request.getClosingObservation(),
                request.getCancellationReason(),
                request.getRejectionReason(),
                request.getDeadline(),
                request.isAiSuggested(),
                toResponse(summary.requestType()),
                toResponse(summary.originChannel()),
                userRestMapper().toResponse(summary.requester()),
                summary.assignedTo().map(userRestMapper()::toResponse).orElse(null)
        );
    }

    default RequestDetailResponse toDetailResponse(RequestDetail detail) {
        Objects.requireNonNull(detail, "El detalle de solicitud no puede ser null");

        var base = toResponse(new RequestSummary(
                detail.request(),
                detail.requestType(),
                detail.originChannel(),
                detail.requester(),
                detail.assignedTo()
        ));

        return new RequestDetailResponse(
                base.id(),
                base.description(),
                base.registrationDateTime(),
                base.status(),
                base.priority(),
                base.priorityJustification(),
                base.closingObservation(),
                base.cancellationReason(),
                base.rejectionReason(),
                base.deadline(),
                base.aiSuggested(),
                base.requestType(),
                base.originChannel(),
                base.requester(),
                base.assignedTo(),
                detail.history().stream().map(this::toResponse).toList()
        );
    }

    default PagedRequestResponse toPagedResponse(RequestPage<RequestSummary> page) {
        Objects.requireNonNull(page, "La página de solicitudes no puede ser null");
        return new PagedRequestResponse(
                page.content().stream().map(this::toResponse).toList(),
                page.totalElements(),
                page.totalPages(),
                page.currentPage(),
                page.pageSize()
        );
    }

    default HistoryEntryResponse toResponse(RequestHistory entry) {
        return new HistoryEntryResponse(
                entry.getId() == null ? null : entry.getId().value(),
                entry.getAction(),
                entry.getObservations(),
                entry.getTimestamp(),
                null // User details not available in raw history
        );
    }

    default HistoryEntryResponse toResponse(RequestHistoryDetail historyDetail) {
        var historyEntry = historyDetail.historyEntry();
        return new HistoryEntryResponse(
                historyEntry.getId() == null ? null : historyEntry.getId().value(),
                historyEntry.getAction(),
                historyEntry.getObservations(),
                historyEntry.getTimestamp(),
                userRestMapper().toResponse(historyDetail.performedBy())
        );
    }

    @Mapping(target = "id", source = "id.value")
    RequestTypeResponse toResponse(RequestType requestType);

    @Mapping(target = "id", source = "id.value")
    OriginChannelResponse toResponse(OriginChannel originChannel);

    private static UserRestMapper userRestMapper() {
        return Holder.USER_REST_MAPPER;
    }

    final class Holder {
        private static final UserRestMapper USER_REST_MAPPER = new UserRestMapper();

        private Holder() {
        }
    }
}
