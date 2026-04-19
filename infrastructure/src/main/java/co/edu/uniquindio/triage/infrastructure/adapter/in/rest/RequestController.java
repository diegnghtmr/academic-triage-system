package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.request.*;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.*;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.RequestRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.HttpIdempotencySupport;
import co.edu.uniquindio.triage.infrastructure.idempotency.OperationScope;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/requests")
class RequestController {

    private final CreateRequestUseCase createRequestUseCase;
    private final ClassifyRequestUseCase classifyRequestUseCase;
    private final PrioritizeRequestUseCase prioritizeRequestUseCase;
    private final AssignRequestUseCase assignRequestUseCase;
    private final AttendRequestUseCase attendRequestUseCase;
    private final CloseRequestUseCase closeRequestUseCase;
    private final CancelRequestUseCase cancelRequestUseCase;
    private final RejectRequestUseCase rejectRequestUseCase;
    private final ListRequestsQuery listRequestsQuery;
    private final GetRequestDetailQuery getRequestDetailQuery;
    private final GetPrioritySuggestionQuery getPrioritySuggestionQuery;
    private final AddInternalNoteUseCase addInternalNoteUseCase;
    private final RequestRestMapper requestRestMapper;
    private final AuthenticatedActorMapper authenticatedActorMapper;
    private final HttpIdempotencySupport httpIdempotencySupport;

    public RequestController(CreateRequestUseCase createRequestUseCase,
                             ClassifyRequestUseCase classifyRequestUseCase,
                             PrioritizeRequestUseCase prioritizeRequestUseCase,
                             AssignRequestUseCase assignRequestUseCase,
                             AttendRequestUseCase attendRequestUseCase,
                             CloseRequestUseCase closeRequestUseCase,
                             CancelRequestUseCase cancelRequestUseCase,
                             RejectRequestUseCase rejectRequestUseCase,
                             ListRequestsQuery listRequestsQuery,
                             GetRequestDetailQuery getRequestDetailQuery,
                             GetPrioritySuggestionQuery getPrioritySuggestionQuery,
                             AddInternalNoteUseCase addInternalNoteUseCase,
                             RequestRestMapper requestRestMapper,
                             AuthenticatedActorMapper authenticatedActorMapper,
                             HttpIdempotencySupport httpIdempotencySupport) {
        this.createRequestUseCase = Objects.requireNonNull(createRequestUseCase);
        this.classifyRequestUseCase = Objects.requireNonNull(classifyRequestUseCase);
        this.prioritizeRequestUseCase = Objects.requireNonNull(prioritizeRequestUseCase);
        this.assignRequestUseCase = Objects.requireNonNull(assignRequestUseCase);
        this.attendRequestUseCase = Objects.requireNonNull(attendRequestUseCase);
        this.closeRequestUseCase = Objects.requireNonNull(closeRequestUseCase);
        this.cancelRequestUseCase = Objects.requireNonNull(cancelRequestUseCase);
        this.rejectRequestUseCase = Objects.requireNonNull(rejectRequestUseCase);
        this.listRequestsQuery = Objects.requireNonNull(listRequestsQuery);
        this.getRequestDetailQuery = Objects.requireNonNull(getRequestDetailQuery);
        this.getPrioritySuggestionQuery = Objects.requireNonNull(getPrioritySuggestionQuery);
        this.addInternalNoteUseCase = Objects.requireNonNull(addInternalNoteUseCase);
        this.requestRestMapper = Objects.requireNonNull(requestRestMapper);
        this.authenticatedActorMapper = Objects.requireNonNull(authenticatedActorMapper);
        this.httpIdempotencySupport = Objects.requireNonNull(httpIdempotencySupport);
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateRequestRequest request,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        return httpIdempotencySupport.execute(
                idempotencyKey,
                OperationScope.REQUESTS_CREATE,
                "POST", "/api/v1/requests",
                String.valueOf(actor.userId().value()),
                MediaType.APPLICATION_JSON_VALUE,
                request,
                () -> {
                    var created = createRequestUseCase.execute(requestRestMapper.toCommand(request), actor);
                    var response = requestRestMapper.toResponse(created);
                    return ResponseEntity.created(URI.create("/api/v1/requests/" + response.id())).body(response);
                }
        );
    }

    @PatchMapping("/{requestId}/classify")
    public ResponseEntity<?> classify(
            @PathVariable("requestId") Long requestId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ClassifyRequestRequest request,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        return httpIdempotencySupport.execute(
                idempotencyKey,
                OperationScope.REQUESTS_CLASSIFY,
                "PATCH", "/api/v1/requests/" + requestId + "/classify",
                String.valueOf(actor.userId().value()),
                MediaType.APPLICATION_JSON_VALUE,
                request,
                () -> ResponseEntity.ok(requestRestMapper.toResponse(
                        classifyRequestUseCase.execute(requestRestMapper.toCommand(requestId, request), actor)
                ))
        );
    }

    @PatchMapping("/{requestId}/prioritize")
    public ResponseEntity<?> prioritize(
            @PathVariable("requestId") Long requestId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PrioritizeRequestRequest request,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        return httpIdempotencySupport.execute(
                idempotencyKey,
                OperationScope.REQUESTS_PRIORITIZE,
                "PATCH", "/api/v1/requests/" + requestId + "/prioritize",
                String.valueOf(actor.userId().value()),
                MediaType.APPLICATION_JSON_VALUE,
                request,
                () -> ResponseEntity.ok(requestRestMapper.toResponse(
                        prioritizeRequestUseCase.execute(requestRestMapper.toCommand(requestId, request), actor)
                ))
        );
    }

    @PatchMapping("/{requestId}/assign")
    public ResponseEntity<?> assign(
            @PathVariable("requestId") Long requestId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AssignRequestRequest request,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        return httpIdempotencySupport.execute(
                idempotencyKey,
                OperationScope.REQUESTS_ASSIGN,
                "PATCH", "/api/v1/requests/" + requestId + "/assign",
                String.valueOf(actor.userId().value()),
                MediaType.APPLICATION_JSON_VALUE,
                request,
                () -> ResponseEntity.ok(requestRestMapper.toResponse(
                        assignRequestUseCase.execute(requestRestMapper.toCommand(requestId, request), actor)
                ))
        );
    }

    @PatchMapping("/{requestId}/attend")
    public ResponseEntity<?> attend(
            @PathVariable("requestId") Long requestId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AttendRequestRequest request,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        return httpIdempotencySupport.execute(
                idempotencyKey,
                OperationScope.REQUESTS_ATTEND,
                "PATCH", "/api/v1/requests/" + requestId + "/attend",
                String.valueOf(actor.userId().value()),
                MediaType.APPLICATION_JSON_VALUE,
                request,
                () -> ResponseEntity.ok(requestRestMapper.toResponse(
                        attendRequestUseCase.execute(requestRestMapper.toCommand(requestId, request), actor)
                ))
        );
    }

    @PatchMapping("/{requestId}/close")
    public ResponseEntity<?> close(
            @PathVariable("requestId") Long requestId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CloseRequestRequest request,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        return httpIdempotencySupport.execute(
                idempotencyKey,
                OperationScope.REQUESTS_CLOSE,
                "PATCH", "/api/v1/requests/" + requestId + "/close",
                String.valueOf(actor.userId().value()),
                MediaType.APPLICATION_JSON_VALUE,
                request,
                () -> ResponseEntity.ok(requestRestMapper.toResponse(
                        closeRequestUseCase.execute(requestRestMapper.toCommand(requestId, request), actor)
                ))
        );
    }

    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<?> cancel(
            @PathVariable("requestId") Long requestId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CancelRequestRequest request,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        return httpIdempotencySupport.execute(
                idempotencyKey,
                OperationScope.REQUESTS_CANCEL,
                "PATCH", "/api/v1/requests/" + requestId + "/cancel",
                String.valueOf(actor.userId().value()),
                MediaType.APPLICATION_JSON_VALUE,
                request,
                () -> ResponseEntity.ok(requestRestMapper.toResponse(
                        cancelRequestUseCase.execute(requestRestMapper.toCommand(requestId, request), actor)
                ))
        );
    }

    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<?> reject(
            @PathVariable("requestId") Long requestId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RejectRequestRequest request,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        return httpIdempotencySupport.execute(
                idempotencyKey,
                OperationScope.REQUESTS_REJECT,
                "PATCH", "/api/v1/requests/" + requestId + "/reject",
                String.valueOf(actor.userId().value()),
                MediaType.APPLICATION_JSON_VALUE,
                request,
                () -> ResponseEntity.ok(requestRestMapper.toResponse(
                        rejectRequestUseCase.execute(requestRestMapper.toCommand(requestId, request), actor)
                ))
        );
    }

    @GetMapping
    public ResponseEntity<PagedRequestResponse> list(
            @RequestParam(name = "status") Optional<RequestStatus> status,
            @RequestParam(name = "requestTypeId") Optional<Long> requestTypeId,
            @RequestParam(name = "priority") Optional<Priority> priority,
            @RequestParam(name = "assignedToUserId") Optional<Long> assignedToUserId,
            @RequestParam(name = "requesterUserId") Optional<Long> requesterUserId,
            @RequestParam(name = "dateFrom") Optional<LocalDate> dateFrom,
            @RequestParam(name = "dateTo") Optional<LocalDate> dateTo,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sort", defaultValue = "registrationDateTime,desc") String sort,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var query = requestRestMapper.toQueryModel(
                status, requestTypeId, priority, assignedToUserId, requesterUserId, dateFrom, dateTo, page, size, sort
        );
        return ResponseEntity.ok(requestRestMapper.toPagedResponse(listRequestsQuery.execute(query, actor)));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<RequestDetailResponse> getById(
            @PathVariable("requestId") Long requestId,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var detail = getRequestDetailQuery.execute(requestRestMapper.toDetailQuery(requestId), actor);
        return ResponseEntity.ok(requestRestMapper.toDetailResponse(detail));
    }

    @GetMapping("/{requestId}/priority-suggestion")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'STUDENT')")
    public ResponseEntity<PrioritySuggestionResponse> getPrioritySuggestion(
            @PathVariable("requestId") Long requestId,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var result = getPrioritySuggestionQuery.execute(requestRestMapper.toPrioritySuggestionQuery(requestId), actor);
        return ResponseEntity.ok(requestRestMapper.toPrioritySuggestionResponse(result));
    }

    @GetMapping("/{requestId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'STUDENT')")
    public ResponseEntity<List<HistoryEntryResponse>> getHistory(
            @PathVariable("requestId") Long requestId,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var detail = getRequestDetailQuery.execute(requestRestMapper.toDetailQuery(requestId), actor);

        if (actor.role() == Role.STUDENT && !detail.requester().getUsername().value().equals(actor.username())) {
            throw new AccessDeniedException("No tienes permiso para ver el historial de esta solicitud.");
        }

        return ResponseEntity.ok(detail.history().stream().map(requestRestMapper::toResponse).toList());
    }

    @PostMapping("/{requestId}/history")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<?> addInternalNote(
            @PathVariable("requestId") Long requestId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AddInternalNoteRequest request,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        return httpIdempotencySupport.execute(
                idempotencyKey,
                OperationScope.REQUESTS_ADD_INTERNAL_NOTE,
                "POST", "/api/v1/requests/" + requestId + "/history",
                String.valueOf(actor.userId().value()),
                MediaType.APPLICATION_JSON_VALUE,
                request,
                () -> {
                    var createdEntry = addInternalNoteUseCase.addInternalNote(
                            requestRestMapper.toCommand(requestId, request, actor.userId()));
                    return ResponseEntity.status(HttpStatus.CREATED).body(requestRestMapper.toResponse(createdEntry));
                }
        );
    }
}
