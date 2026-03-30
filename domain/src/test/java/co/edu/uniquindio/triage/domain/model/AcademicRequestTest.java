package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcademicRequestTest {

    @Test
    void classifyMustAcceptOptionalObservationsAndAppendHistoryWithoutExternalId() {
        var request = newRequest();
        var classifierId = new UserId(20L);
        var classifiedAt = LocalDateTime.of(2026, 3, 24, 9, 30);

        request.classify(new RequestTypeId(2L), "  Revisado por mesa de ayuda  ", classifierId, classifiedAt);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.CLASSIFIED);
        assertThat(request.getRequestTypeId()).isEqualTo(new RequestTypeId(2L));

        var historyEntry = request.getHistory().getLast();
        assertThat(historyEntry.getId()).isNull();
        assertThat(historyEntry.getAction()).isEqualTo(HistoryAction.CLASSIFIED);
        assertThat(historyEntry.getObservations()).isEqualTo("Revisado por mesa de ayuda");
        assertThat(historyEntry.getPerformedById()).isEqualTo(classifierId);
    }

    @Test
    void prioritizeMustPersistTrimmedJustificationInAggregateAndHistory() {
        var request = classifiedRequest();
        var prioritizedAt = LocalDateTime.of(2026, 3, 24, 10, 0);
        var prioritizerId = new UserId(21L);

        request.prioritize(Priority.HIGH, "  Urgente por cierre de semestre  ", prioritizerId, prioritizedAt);

        assertThat(request.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(request.getPriorityJustification()).isEqualTo("Urgente por cierre de semestre");

        var historyEntry = request.getHistory().getLast();
        assertThat(historyEntry.getAction()).isEqualTo(HistoryAction.PRIORITIZED);
        assertThat(historyEntry.getObservations()).isEqualTo("Urgente por cierre de semestre");
        assertThat(historyEntry.getId()).isNull();
    }

    @Test
    void assignMustRejectRequestsThatWereNotPrioritized() {
        var request = classifiedRequest();

        assertThatThrownBy(() -> request.assign(staffUser(30L, true), new UserId(99L), null, LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("priorizada");
    }

    @Test
    void assignMustRejectInactiveOrNonStaffAssignees() {
        var request = prioritizedRequest();

        assertThatThrownBy(() -> request.assign(staffUser(31L, false), new UserId(99L), null, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("activo");

        assertThatThrownBy(() -> request.assign(studentUser(32L), new UserId(99L), null, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rol STAFF");
    }

    @Test
    void assignMustRecordActingStaffSeparatelyFromAssignee() {
        var request = prioritizedRequest();
        var actorId = new UserId(40L);
        var assignee = staffUser(41L, true);
        var assignedAt = LocalDateTime.of(2026, 3, 24, 11, 0);

        request.assign(assignee, actorId, "  Caso derivado al analista  ", assignedAt);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.IN_PROGRESS);
        var assigneeId = assignee.getId().orElseThrow();
        assertThat(request.getResponsibleId()).isEqualTo(assigneeId);

        var historyEntry = request.getHistory().getLast();
        assertThat(historyEntry.getAction()).isEqualTo(HistoryAction.ASSIGNED);
        assertThat(historyEntry.getPerformedById()).isEqualTo(actorId);
        assertThat(historyEntry.getResponsibleId()).isEqualTo(assigneeId);
        assertThat(historyEntry.getObservations()).isEqualTo("Caso derivado al analista");
        assertThat(historyEntry.getId()).isNull();
    }

    @Test
    void attendMustTransitionInProgressRequestAndPreserveTrimmedObservationInAuditTrail() {
        var request = inProgressRequest();
        var actorId = new UserId(50L);
        var attendedAt = LocalDateTime.of(2026, 3, 24, 12, 0);

        request.attend("  Solicitud atendida con soporte entregado al estudiante  ", actorId, attendedAt);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.ATTENDED);
        assertThat(request.getAttendanceObservation()).isEqualTo("Solicitud atendida con soporte entregado al estudiante");

        var historyEntry = request.getHistory().getLast();
        assertThat(historyEntry.getAction()).isEqualTo(HistoryAction.ATTENDED);
        assertThat(historyEntry.getObservations()).isEqualTo("Solicitud atendida con soporte entregado al estudiante");
        assertThat(historyEntry.getPerformedById()).isEqualTo(actorId);
    }

    @Test
    void attendMustRejectRequestsOutsideInProgressState() {
        var request = prioritizedRequest();

        assertThatThrownBy(() -> request.attend("Observación válida", new UserId(51L), LocalDateTime.now()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ATTENDED");
    }

    @Test
    void closeMustTransitionAttendedRequestAndAcceptTwoThousandCharacters() {
        var request = attendedRequest();
        var actorId = new UserId(52L);
        var closedAt = LocalDateTime.of(2026, 3, 24, 13, 0);
        var observation = "x".repeat(2000);

        request.close(observation, actorId, closedAt);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.CLOSED);
        assertThat(request.getClosingObservation()).isEqualTo(observation);

        var historyEntry = request.getHistory().getLast();
        assertThat(historyEntry.getAction()).isEqualTo(HistoryAction.CLOSED);
        assertThat(historyEntry.getObservations()).isEqualTo(observation);
        assertThat(historyEntry.getPerformedById()).isEqualTo(actorId);
    }

    @Test
    void closeMustRejectBlankObservation() {
        var request = attendedRequest();

        assertThatThrownBy(() -> request.close("   ", new UserId(53L), LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cierre");
    }

    @Test
    void closeMustRejectRequestsOutsideAttendedState() {
        var request = inProgressRequest();

        assertThatThrownBy(() -> request.close("Observación válida", new UserId(54L), LocalDateTime.now()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void cancelMustAllowRegisteredRequestAndPersistTrimmedReasonInHistory() {
        var request = newRequest();
        var actorId = new UserId(60L);
        var cancelledAt = LocalDateTime.of(2026, 3, 24, 14, 0);

        request.cancel("  El estudiante resolvió el trámite por otra vía  ", actorId, cancelledAt);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(request.getCancellationReason()).isEqualTo("El estudiante resolvió el trámite por otra vía");

        var historyEntry = request.getHistory().getLast();
        assertThat(historyEntry.getAction()).isEqualTo(HistoryAction.CANCELLED);
        assertThat(historyEntry.getObservations()).isEqualTo("El estudiante resolvió el trámite por otra vía");
        assertThat(historyEntry.getPerformedById()).isEqualTo(actorId);
    }

    @Test
    void cancelMustAllowClassifiedRequest() {
        var request = classifiedRequest();

        request.cancel("Solicitud retirada por el solicitante", new UserId(61L), LocalDateTime.of(2026, 3, 24, 14, 5));

        assertThat(request.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(request.getCancellationReason()).isEqualTo("Solicitud retirada por el solicitante");
    }

    @Test
    void cancelMustRejectBlankOrOversizedReason() {
        var blankReasonRequest = newRequest();
        var oversizedReasonRequest = newRequest();
        var oversizedReason = "x".repeat(2001);

        assertThatThrownBy(() -> blankReasonRequest.cancel("   ", new UserId(62L), LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("razón");

        assertThatThrownBy(() -> oversizedReasonRequest.cancel(oversizedReason, new UserId(62L), LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 5 y 2000");
    }

    @Test
    void cancelMustRejectRequestsOutsideRegisteredOrClassifiedStates() {
        var request = inProgressRequest();

        assertThatThrownBy(() -> request.cancel("Motivo válido", new UserId(63L), LocalDateTime.now()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void rejectMustAllowRegisteredRequestAndPersistTrimmedReasonInHistory() {
        var request = newRequest();
        var actorId = new UserId(64L);
        var rejectedAt = LocalDateTime.of(2026, 3, 24, 14, 10);

        request.reject("  La documentación aportada no cumple los requisitos mínimos  ", actorId, rejectedAt);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(request.getRejectionReason()).isEqualTo("La documentación aportada no cumple los requisitos mínimos");

        var historyEntry = request.getHistory().getLast();
        assertThat(historyEntry.getAction()).isEqualTo(HistoryAction.REJECTED);
        assertThat(historyEntry.getObservations()).isEqualTo("La documentación aportada no cumple los requisitos mínimos");
        assertThat(historyEntry.getPerformedById()).isEqualTo(actorId);
    }

    @Test
    void rejectMustRequireActorId() {
        var request = newRequest();

        assertThatThrownBy(() -> request.reject("Motivo válido", null, LocalDateTime.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("rechaza");
    }

    @Test
    void rejectMustRejectBlankOrOversizedReason() {
        var blankReasonRequest = newRequest();
        var oversizedReasonRequest = newRequest();
        var oversizedReason = "x".repeat(2001);

        assertThatThrownBy(() -> blankReasonRequest.reject("   ", new UserId(65L), LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("razón");

        assertThatThrownBy(() -> oversizedReasonRequest.reject(oversizedReason, new UserId(65L), LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 5 y 2000");
    }

    @Test
    void rejectMustRejectRequestsOutsideRegisteredState() {
        var request = classifiedRequest();

        assertThatThrownBy(() -> request.reject("Motivo válido", new UserId(66L), LocalDateTime.now()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("REJECTED");
    }

    @Test
    void addInternalNoteMustAppendHistoryWithoutChangingStatus() {
        var request = newRequest();
        var initialStatus = request.getStatus();
        var actorId = new UserId(70L);
        var timestamp = LocalDateTime.of(2026, 3, 24, 15, 0);
        var note = "Esta es una nota interna de seguimiento";

        request.addInternalNote(note, actorId, timestamp);

        assertThat(request.getStatus()).isEqualTo(initialStatus);
        var historyEntry = request.getHistory().getLast();
        assertThat(historyEntry.getAction()).isEqualTo(HistoryAction.INTERNAL_NOTE);
        assertThat(historyEntry.getObservations()).isEqualTo(note);
        assertThat(historyEntry.getPerformedById()).isEqualTo(actorId);
        assertThat(historyEntry.getTimestamp()).isEqualTo(timestamp);
    }

    @Test
    void addInternalNoteMustRejectBlankOrOversizedNote() {
        var request = newRequest();
        var actorId = new UserId(71L);
        var oversizedNote = "x".repeat(2001);

        assertThatThrownBy(() -> request.addInternalNote("   ", actorId, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nota interna");

        assertThatThrownBy(() -> request.addInternalNote(oversizedNote, actorId, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entre 1 y 2000");
    }

    private AcademicRequest newRequest() {
        return new AcademicRequest(
                new RequestId(1L),
                "Solicitud de apoyo para homologación académica",
                new UserId(10L),
                new OriginChannelId(1L),
                new RequestTypeId(1L),
                LocalDate.of(2026, 4, 10),
                false,
                LocalDateTime.of(2026, 3, 24, 8, 0)
        );
    }

    private AcademicRequest classifiedRequest() {
        var request = newRequest();
        request.classify(new RequestTypeId(2L), (String) null, new UserId(20L), LocalDateTime.of(2026, 3, 24, 8, 30));
        return request;
    }

    private AcademicRequest prioritizedRequest() {
        var request = classifiedRequest();
        request.prioritize(Priority.MEDIUM, "Requiere revisión del equipo académico", new UserId(21L), LocalDateTime.of(2026, 3, 24, 9, 0));
        return request;
    }

    private AcademicRequest inProgressRequest() {
        var request = prioritizedRequest();
        request.assign(staffUser(41L, true), new UserId(40L), "Asignada para atención", LocalDateTime.of(2026, 3, 24, 10, 0));
        return request;
    }

    private AcademicRequest attendedRequest() {
        var request = inProgressRequest();
        request.attend("Atención inicial completada", new UserId(42L), LocalDateTime.of(2026, 3, 24, 11, 0));
        return request;
    }

    private User staffUser(Long id, boolean active) {
        return user(id, Role.STAFF, active);
    }

    private User studentUser(Long id) {
        return user(id, Role.STUDENT, true);
    }

    private User user(Long id, Role role, boolean active) {
        return User.reconstitute(
                new UserId(id),
                new Username("user" + id),
                "Ana",
                "Gómez",
                new PasswordHash("hash-value"),
                new Identification("1094" + id),
                new Email("user" + id + "@uniquindio.edu.co"),
                role,
                active
        );
    }
}
