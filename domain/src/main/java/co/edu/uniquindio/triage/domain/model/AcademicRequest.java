package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.PriorityEnum;
import co.edu.uniquindio.triage.domain.enums.RequestStatusEnum;
import co.edu.uniquindio.triage.domain.exception.InvalidStateTransitionException;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.domain.service.RequestStateMachine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class AcademicRequest {
    private RequestId id;
    private String description;
    private RequestStatusEnum status;
    private PriorityEnum priority;
    private String priorityJustification;
    private LocalDate deadline;
    private LocalDateTime registrationDateTime;
    private boolean aiSuggested;
    private String rejectionReason;
    private String closingObservation;
    private UserId applicantId;
    private UserId responsibleId;
    private OriginChannelId originChannelId;
    private RequestTypeId requestTypeId;
    private List<BusinessRuleId> appliedRuleIds;

    private static final Set<RequestStatusEnum> TERMINAL_STATES = Set.of(
            RequestStatusEnum.CLOSED,
            RequestStatusEnum.CANCELLED,
            RequestStatusEnum.REJECTED
    );

    private static final Set<RequestStatusEnum> CANCELLABLE_STATES = Set.of(
            RequestStatusEnum.REGISTERED,
            RequestStatusEnum.CLASSIFIED
    );

    public AcademicRequest(RequestId id, String description, UserId applicantId,
                           OriginChannelId originChannelId, RequestTypeId requestTypeId,
                           LocalDate deadline, boolean aiSuggested) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
        this.description = validateDescription(description);
        this.status = RequestStatusEnum.REGISTERED;
        this.priority = null;
        this.priorityJustification = null;
        this.deadline = deadline;
        this.registrationDateTime = LocalDateTime.now();
        this.aiSuggested = aiSuggested;
        this.rejectionReason = null;
        this.closingObservation = null;
        this.applicantId = Objects.requireNonNull(applicantId, "El applicantId no puede ser null");
        this.responsibleId = null;
        this.originChannelId = Objects.requireNonNull(originChannelId, "El originChannelId no puede ser null");
        this.requestTypeId = Objects.requireNonNull(requestTypeId, "El requestTypeId no puede ser null");
        this.appliedRuleIds = new ArrayList<>();
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

    public RequestId getId() {
        return id;
    }

    public void setId(RequestId id) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
    }

    public void setResponsibleId(UserId responsibleId) {
        this.responsibleId = responsibleId;
    }

    public void setRequestTypeId(RequestTypeId requestTypeId) {
        this.requestTypeId = requestTypeId;
    }

    public void setOriginChannelId(OriginChannelId originChannelId) {
        this.originChannelId = originChannelId;
    }

    public String getDescription() {
        return description;
    }

    public RequestStatusEnum getStatus() {
        return status;
    }

    public PriorityEnum getPriority() {
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

    public void clasificar(RequestTypeId newRequestTypeId) {
        RequestStateMachine.validarTransicion(this.status, RequestStatusEnum.CLASSIFIED);
        this.requestTypeId = Objects.requireNonNull(newRequestTypeId, "El requestTypeId no puede ser null");
        this.status = RequestStatusEnum.CLASSIFIED;
    }

    public void priorizar(PriorityEnum newPriority, String justification) {
        RequestStateMachine.validarTransicion(this.status, RequestStatusEnum.IN_PROGRESS);
        if (newPriority == null) {
            throw new IllegalArgumentException("La prioridad no puede ser null");
        }
        this.priorityJustification = validateJustification(justification);
        this.priority = newPriority;
    }

    public void asignarA(UserId staffId) {
        RequestStateMachine.validarTransicion(this.status, RequestStatusEnum.IN_PROGRESS);
        this.responsibleId = Objects.requireNonNull(staffId, "El responsibleId no puede ser null");
        if (this.status == RequestStatusEnum.CLASSIFIED) {
            this.status = RequestStatusEnum.IN_PROGRESS;
        }
    }

    public void atender(String observation) {
        RequestStateMachine.validarTransicion(this.status, RequestStatusEnum.ATTENDED);
        if (observation == null || observation.isBlank()) {
            throw new IllegalArgumentException("La observación no puede ser null o vacía");
        }
        if (observation.length() < 5 || observation.length() > 2000) {
            throw new IllegalArgumentException("La observación debe tener entre 5 y 2000 caracteres");
        }
        this.status = RequestStatusEnum.ATTENDED;
    }

    public void cerrar(String closingObservation) {
        RequestStateMachine.validarTransicion(this.status, RequestStatusEnum.CLOSED);
        this.closingObservation = validateClosingObservation(closingObservation);
        this.status = RequestStatusEnum.CLOSED;
    }

    public void cancelar(String reason) {
        if (!puedeSerCancelada()) {
            throw new InvalidStateTransitionException(
                    String.format("No se puede cancelar la solicitud en estado %s", this.status)
            );
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("La razón de cancelación no puede ser null o vacía");
        }
        if (reason.length() < 5 || reason.length() > 2000) {
            throw new IllegalArgumentException("La razón debe tener entre 5 y 2000 caracteres");
        }
        this.status = RequestStatusEnum.CANCELLED;
    }

    public void rechazar(String reason, UserId rejectedById) {
        if (this.status != RequestStatusEnum.REGISTERED) {
            throw new InvalidStateTransitionException(
                    String.format("Solo se pueden rechazar solicitudes en estado REGISTERED, estado actual: %s", this.status)
            );
        }
        if (rejectedById == null) {
            throw new IllegalArgumentException("El id del usuario que rechaza no puede ser null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("La razón de rechazo no puede ser null o vacía");
        }
        if (reason.length() < 5 || reason.length() > 2000) {
            throw new IllegalArgumentException("La razón debe tener entre 5 y 2000 caracteres");
        }
        this.rejectionReason = reason.trim();
        this.status = RequestStatusEnum.REJECTED;
    }

    public void aplicarRegla(BusinessRuleId ruleId) {
        if (ruleId == null) {
            throw new IllegalArgumentException("El ruleId no puede ser null");
        }
        if (!this.appliedRuleIds.contains(ruleId)) {
            this.appliedRuleIds.add(ruleId);
        }
    }

    public boolean puedeSerCancelada() {
        return CANCELLABLE_STATES.contains(this.status);
    }

    public boolean estaEnEstadoTerminal() {
        return TERMINAL_STATES.contains(this.status);
    }

    public boolean estaPendienteDeClasificacion() {
        return this.status == RequestStatusEnum.REGISTERED;
    }

    public boolean estaPendienteDePriorizacion() {
        return this.status == RequestStatusEnum.CLASSIFIED;
    }

    public boolean estaPendienteDeAsignacion() {
        return this.status == RequestStatusEnum.CLASSIFIED && this.responsibleId == null;
    }

    public boolean estaSinAtender() {
        return this.status == RequestStatusEnum.IN_PROGRESS;
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
