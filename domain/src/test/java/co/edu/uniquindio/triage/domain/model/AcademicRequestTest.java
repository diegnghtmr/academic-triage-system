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
        assertThat(request.getResponsibleId()).isEqualTo(assignee.getId());

        var historyEntry = request.getHistory().getLast();
        assertThat(historyEntry.getAction()).isEqualTo(HistoryAction.ASSIGNED);
        assertThat(historyEntry.getPerformedById()).isEqualTo(actorId);
        assertThat(historyEntry.getResponsibleId()).isEqualTo(assignee.getId());
        assertThat(historyEntry.getObservations()).isEqualTo("Caso derivado al analista");
        assertThat(historyEntry.getId()).isNull();
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
