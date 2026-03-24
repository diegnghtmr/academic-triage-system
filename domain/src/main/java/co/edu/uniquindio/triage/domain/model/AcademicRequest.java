package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestHistoryId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.domain.service.StateTransitionValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AcademicRequest {
    private final RequestId id;
    private final String description;
    private RequestStatus status;
    private Priority priority;
    private String priorityJustification;
    private final LocalDate deadline;
    private final LocalDateTime registrationDateTime;
    private final boolean aiSuggested;
    private String rejectionReason;
    private String closingObservation;
    private String cancellationReason;
    private String attendanceObservation;
    private final UserId applicantId;
    private UserId responsibleId;
    private final OriginChannelId originChannelId;
    private RequestTypeId requestTypeId;
    private final List<BusinessRuleId> appliedRuleIds;
    private final List<RequestHistory> history = new ArrayList<>();

    public AcademicRequest(RequestId id, String description, UserId applicantId,
                           OriginChannelId originChannelId, RequestTypeId requestTypeId,
                           LocalDate deadline, boolean aiSuggested,
                           LocalDateTime registrationDateTime) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
        this.description = validateDescription(description);
        this.status = RequestStatus.REGISTERED;
        this.priority = null;
        this.priorityJustification = null;
        this.deadline = deadline;
        this.registrationDateTime = Objects.requireNonNull(registrationDateTime, "El registrationDateTime no puede ser null");
        this.aiSuggested = aiSuggested;
        this.rejectionReason = null;
        this.closingObservation = null;
        this.cancellationReason = null;
        this.attendanceObservation = null;
        this.applicantId = Objects.requireNonNull(applicantId, "El applicantId no puede ser null");
        this.responsibleId = null;
        this.originChannelId = Objects.requireNonNull(originChannelId, "El originChannelId no puede ser null");
        this.requestTypeId = Objects.requireNonNull(requestTypeId, "El requestTypeId no puede ser null");
        this.appliedRuleIds = new ArrayList<>();
        this.history.add(new RequestHistory(
                null,
                HistoryAction.REGISTERED,
                "Request registered",
                registrationDateTime,
                this.id,
                this.applicantId
        ));
    }

    /**
     * Factory method for ORM/mapper reconstitution. Package-private access.
     */
    @SuppressWarnings("java:S107")
    public static AcademicRequest reconstitute(RequestId id, String description, RequestStatus status,
                                        Priority priority, String priorityJustification,
                                        LocalDate deadline, LocalDateTime registrationDateTime,
                                        boolean aiSuggested, String rejectionReason,
                                        String closingObservation, String cancellationReason,
                                        String attendanceObservation, UserId applicantId,
                                        UserId responsibleId, OriginChannelId originChannelId,
                                        RequestTypeId requestTypeId, List<BusinessRuleId> appliedRuleIds,
                                        List<RequestHistory> history) {
        AcademicRequest request = new AcademicRequest(id, description, applicantId,
                originChannelId, requestTypeId, deadline, aiSuggested, registrationDateTime);
        request.status = status;
        request.priority = priority;
        request.priorityJustification = priorityJustification;
        request.rejectionReason = rejectionReason;
        request.closingObservation = closingObservation;
        request.cancellationReason = cancellationReason;
        request.attendanceObservation = attendanceObservation;
        request.responsibleId = responsibleId;
        if (appliedRuleIds != null) {
            request.appliedRuleIds.addAll(appliedRuleIds);
        }
        request.history.clear();
        if (history != null) {
            request.history.addAll(history);
        }
        return request;
    }

    // --- Business methods ---

    public void classify(RequestTypeId newRequestTypeId, RequestHistoryId historyId,
                         UserId performedById, LocalDateTime timestamp) {
        classify(newRequestTypeId, (String) null, performedById, timestamp);
    }

    public void classify(RequestTypeId newRequestTypeId, String observations,
                         UserId performedById, LocalDateTime timestamp) {
        StateTransitionValidator.validateTransition(this.status, RequestStatus.CLASSIFIED);
        this.requestTypeId = Objects.requireNonNull(newRequestTypeId, "El requestTypeId no puede ser null");
        this.status = RequestStatus.CLASSIFIED;
        addHistory(HistoryAction.CLASSIFIED, observations, timestamp, performedById);
    }

    public void prioritize(Priority newPriority, String justification,
                             RequestHistoryId historyId, UserId performedById, LocalDateTime timestamp) {
        prioritize(newPriority, justification, performedById, timestamp);
    }

    public void prioritize(Priority newPriority, String justification,
                           UserId performedById, LocalDateTime timestamp) {
        if (this.status != RequestStatus.CLASSIFIED) {
            throw new IllegalStateException(
                    String.format("Solo se puede priorizar en estado CLASSIFIED, estado actual: %s", this.status));
        }
        Objects.requireNonNull(newPriority, "La prioridad no puede ser null");
        this.priorityJustification = validateJustification(justification);
        this.priority = newPriority;
        addHistory(HistoryAction.PRIORITIZED, this.priorityJustification, timestamp, performedById);
    }

    public void assign(User staff, RequestHistoryId historyId, LocalDateTime timestamp) {
        assign(staff, staff == null ? null : staff.getId(), null, timestamp);
    }

    public void assign(User staff, UserId performedById, String observations, LocalDateTime timestamp) {
        StateTransitionValidator.validateTransition(this.status, RequestStatus.IN_PROGRESS);
        ensurePrioritizedBeforeAssignment();
        Objects.requireNonNull(staff, "El usuario responsable no puede ser null");
        if (!staff.isActive()) {
            throw new IllegalArgumentException("El usuario responsable debe estar activo");
        }
        if (staff.getRole() != Role.STAFF) {
            throw new IllegalArgumentException("El usuario responsable debe tener rol STAFF");
        }
        this.responsibleId = staff.getId();
        this.status = RequestStatus.IN_PROGRESS;
        addHistory(HistoryAction.ASSIGNED, observations, timestamp, performedById, this.responsibleId);
    }

    public void attend(String observation, RequestHistoryId historyId,
                       UserId performedById, LocalDateTime timestamp) {
        attend(observation, performedById, timestamp);
    }

    public void attend(String observation, UserId performedById, LocalDateTime timestamp) {
        StateTransitionValidator.validateTransition(this.status, RequestStatus.ATTENDED);
        this.attendanceObservation = validateObservation(observation);
        this.status = RequestStatus.ATTENDED;
        addHistory(HistoryAction.ATTENDED, observation, timestamp, performedById);
    }

    public void close(String closingObservation, RequestHistoryId historyId,
                      UserId performedById, LocalDateTime timestamp) {
        close(closingObservation, performedById, timestamp);
    }

    public void close(String closingObservation, UserId performedById, LocalDateTime timestamp) {
        StateTransitionValidator.validateTransition(this.status, RequestStatus.CLOSED);
        this.closingObservation = validateClosingObservation(closingObservation);
        this.status = RequestStatus.CLOSED;
        addHistory(HistoryAction.CLOSED, closingObservation, timestamp, performedById);
    }

    public void cancel(String reason, RequestHistoryId historyId,
                       UserId performedById, LocalDateTime timestamp) {
        cancel(reason, performedById, timestamp);
    }

    public void cancel(String reason, UserId performedById, LocalDateTime timestamp) {
        StateTransitionValidator.validateTransition(this.status, RequestStatus.CANCELLED);
        this.cancellationReason = validateReason(reason);
        this.status = RequestStatus.CANCELLED;
        addHistory(HistoryAction.CANCELLED, reason, timestamp, performedById);
    }

    public void reject(String reason, UserId rejectedById, RequestHistoryId historyId,
                       LocalDateTime timestamp) {
        reject(reason, rejectedById, timestamp);
    }

    public void reject(String reason, UserId rejectedById, LocalDateTime timestamp) {
        StateTransitionValidator.validateTransition(this.status, RequestStatus.REJECTED);
        Objects.requireNonNull(rejectedById, "El id del usuario que rechaza no puede ser null");
        this.rejectionReason = validateReason(reason);
        this.status = RequestStatus.REJECTED;
        addHistory(HistoryAction.REJECTED, reason, timestamp, rejectedById);
    }

    public void applyRule(BusinessRuleId ruleId) {
        Objects.requireNonNull(ruleId, "El ruleId no puede ser null");
        if (!this.appliedRuleIds.contains(ruleId)) {
            this.appliedRuleIds.add(ruleId);
        }
    }

    // --- Query methods ---

    public boolean isCancellable() {
        return StateTransitionValidator.canTransition(this.status, RequestStatus.CANCELLED);
    }

    public boolean isTerminal() {
        return StateTransitionValidator.isTerminal(this.status);
    }

    public boolean isPendingClassification() {
        return this.status == RequestStatus.REGISTERED;
    }

    public boolean isPendingPrioritization() {
        return this.status == RequestStatus.CLASSIFIED;
    }

    public boolean isPendingAssignment() {
        return this.status == RequestStatus.CLASSIFIED && this.responsibleId == null && isPrioritized();
    }

    public boolean isUnattended() {
        return this.status == RequestStatus.IN_PROGRESS;
    }

    // --- Getters ---

    public RequestId getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getPriorityJustification() {
        return priorityJustification;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public LocalDateTime getRegistrationDateTime() {
        return registrationDateTime;
    }

    public boolean isAiSuggested() {
        return aiSuggested;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getClosingObservation() {
        return closingObservation;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public String getAttendanceObservation() {
        return attendanceObservation;
    }

    public UserId getApplicantId() {
        return applicantId;
    }

    public UserId getResponsibleId() {
        return responsibleId;
    }

    public OriginChannelId getOriginChannelId() {
        return originChannelId;
    }

    public RequestTypeId getRequestTypeId() {
        return requestTypeId;
    }

    public List<BusinessRuleId> getAppliedRuleIds() {
        return List.copyOf(appliedRuleIds);
    }

    public List<RequestHistory> getHistory() {
        return List.copyOf(history);
    }

    // --- Validation helpers ---

    private void addHistory(HistoryAction action, String observations,
                            LocalDateTime timestamp, UserId performedById) {
        addHistory(action, observations, timestamp, performedById, null);
    }

    private void addHistory(HistoryAction action, String observations,
                            LocalDateTime timestamp, UserId performedById, UserId responsibleId) {
        this.history.add(new RequestHistory(
                null,
                action, observations,
                Objects.requireNonNull(timestamp, "El timestamp no puede ser null"),
                this.id,
                Objects.requireNonNull(performedById, "El performedById no puede ser null"),
                responsibleId
        ));
    }

    private void ensurePrioritizedBeforeAssignment() {
        if (!isPrioritized()) {
            throw new IllegalStateException("La solicitud debe estar priorizada antes de asignarse");
        }
    }

    private boolean isPrioritized() {
        return this.priority != null && this.priorityJustification != null && !this.priorityJustification.isBlank();
    }

    private String validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("La descripción no puede ser null o vacía");
        }
        String trimmed = description.trim();
        if (trimmed.length() < 10 || trimmed.length() > 2000) {
            throw new IllegalArgumentException("La descripción debe tener entre 10 y 2000 caracteres");
        }
        return trimmed;
    }

    private String validateJustification(String justification) {
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException("La justificación no puede ser null o vacía");
        }
        String trimmed = justification.trim();
        if (trimmed.length() < 5 || trimmed.length() > 1000) {
            throw new IllegalArgumentException("La justificación debe tener entre 5 y 1000 caracteres");
        }
        return trimmed;
    }

    private String validateClosingObservation(String observation) {
        if (observation == null || observation.isBlank()) {
            throw new IllegalArgumentException("La observación de cierre no puede ser null o vacía");
        }
        String trimmed = observation.trim();
        if (trimmed.length() < 5 || trimmed.length() > 2000) {
            throw new IllegalArgumentException("La observación de cierre debe tener entre 5 y 2000 caracteres");
        }
        return trimmed;
    }

    private String validateObservation(String observation) {
        if (observation == null || observation.isBlank()) {
            throw new IllegalArgumentException("La observación no puede ser null o vacía");
        }
        String trimmed = observation.trim();
        if (trimmed.length() < 5 || trimmed.length() > 2000) {
            throw new IllegalArgumentException("La observación debe tener entre 5 y 2000 caracteres");
        }
        return trimmed;
    }

    private String validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("La razón no puede ser null o vacía");
        }
        String trimmed = reason.trim();
        if (trimmed.length() < 5 || trimmed.length() > 2000) {
            throw new IllegalArgumentException("La razón debe tener entre 5 y 2000 caracteres");
        }
        return trimmed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AcademicRequest that = (AcademicRequest) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AcademicRequest{" +
                "id=" + id +
                ", status=" + status +
                ", priority=" + priority +
                ", applicantId=" + applicantId +
                '}';
    }
}
