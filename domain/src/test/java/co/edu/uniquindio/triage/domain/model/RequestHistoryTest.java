package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.model.id.RequestHistoryId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class RequestHistoryTest {

    @Test
    @DisplayName("Should create RequestHistory and maintain integrity")
    void shouldCreateRequestHistory() {
        RequestHistoryId id = new RequestHistoryId(1L);
        RequestId requestId = new RequestId(10L);
        UserId performedById = new UserId(5L);
        UserId responsibleId = new UserId(6L);
        LocalDateTime now = LocalDateTime.now();

        RequestHistory history = new RequestHistory(id, HistoryAction.CLASSIFIED, "Classified manually", now, requestId, performedById, responsibleId);

        assertEquals(id, history.getId());
        assertEquals(HistoryAction.CLASSIFIED, history.getAction());
        assertEquals("Classified manually", history.getObservations());
        assertEquals(now, history.getTimestamp());
        assertEquals(requestId, history.getRequestId());
        assertEquals(performedById, history.getPerformedById());
        assertEquals(responsibleId, history.getResponsibleId());
    }

    @Test
    @DisplayName("Should validate equality and hashCode based on ID")
    void shouldValidateEquality() {
        RequestHistoryId id1 = new RequestHistoryId(1L);
        RequestHistoryId id2 = new RequestHistoryId(1L);
        RequestHistoryId id3 = new RequestHistoryId(2L);

        RequestHistory h1 = new RequestHistory(id1, HistoryAction.REGISTERED, "Obs", LocalDateTime.now(), new RequestId(1L), new UserId(1L));
        RequestHistory h2 = new RequestHistory(id2, HistoryAction.CLASSIFIED, "Obs diff", LocalDateTime.now().plusHours(1), new RequestId(2L), new UserId(2L));
        RequestHistory h3 = new RequestHistory(id3, HistoryAction.REGISTERED, "Obs", LocalDateTime.now(), new RequestId(1L), new UserId(1L));

        assertEquals(h1, h2);
        assertNotEquals(h1, h3);
        assertEquals(h1.hashCode(), h2.hashCode());
    }

    @Test
    @DisplayName("Should normalize observations (trim and nullify empty)")
    void shouldNormalizeObservations() {
        RequestHistoryId id = new RequestHistoryId(1L);
        RequestId requestId = new RequestId(10L);
        UserId performedById = new UserId(5L);
        LocalDateTime now = LocalDateTime.now();

        RequestHistory h1 = new RequestHistory(id, HistoryAction.REGISTERED, "  trimmed  ", now, requestId, performedById);
        RequestHistory h2 = new RequestHistory(id, HistoryAction.REGISTERED, "   ", now, requestId, performedById);
        RequestHistory h3 = new RequestHistory(id, HistoryAction.REGISTERED, null, now, requestId, performedById);

        assertEquals("trimmed", h1.getObservations());
        assertNull(h2.getObservations());
        assertNull(h3.getObservations());
    }

    @Test
    @DisplayName("Should throw exception for oversized observations")
    void shouldThrowForOversizedObservations() {
        RequestHistoryId id = new RequestHistoryId(1L);
        RequestId requestId = new RequestId(10L);
        UserId performedById = new UserId(5L);
        String longObs = "a".repeat(2001);

        assertThatThrownBy(
                () -> new RequestHistory(
                        id, HistoryAction.REGISTERED, longObs, LocalDateTime.now(), requestId, performedById))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2000");
    }

    @Test
    void equalsUsesIdWhenBothPresent() {
        var id = new RequestHistoryId(42L);
        var ts = LocalDateTime.of(2026, 4, 1, 12, 0);
        var a = new RequestHistory(id, HistoryAction.REGISTERED, "A", ts, new RequestId(1L), new UserId(1L));
        var b = new RequestHistory(new RequestHistoryId(42L), HistoryAction.CLASSIFIED, "B", ts.plusDays(1), new RequestId(9L), new UserId(9L));
        var c = new RequestHistory(new RequestHistoryId(43L), HistoryAction.REGISTERED, "A", ts, new RequestId(1L), new UserId(1L));

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void equalsFallsBackToValueFieldsWhenIdsAbsent() {
        var ts = LocalDateTime.of(2026, 4, 2, 10, 0);
        var reqId = new RequestId(5L);
        var actor = new UserId(8L);
        var resp = new UserId(9L);
        var base = new RequestHistory(null, HistoryAction.ASSIGNED, "Nota", ts, reqId, actor, resp);
        var same = new RequestHistory(null, HistoryAction.ASSIGNED, "Nota", ts, reqId, actor, resp);
        var diffObs = new RequestHistory(null, HistoryAction.ASSIGNED, "Otra", ts, reqId, actor, resp);

        assertThat(base).isEqualTo(same);
        assertThat(base).isNotEqualTo(diffObs);
    }

    @Test
    void equalsRejectsNullAndForeignType() {
        var h = new RequestHistory(null, HistoryAction.REGISTERED, "x", LocalDateTime.now(), new RequestId(1L), new UserId(1L));
        assertThat(h.equals(null)).isFalse();
        assertThat(h.equals("x")).isFalse();
    }

    @Test
    void hashCodeIsConsistentWithEqualsWhenIdsPresent() {
        var id = new RequestHistoryId(7L);
        var ts = LocalDateTime.of(2026, 4, 3, 8, 0);
        var a = new RequestHistory(id, HistoryAction.REGISTERED, "a", ts, new RequestId(2L), new UserId(3L));
        var b = new RequestHistory(new RequestHistoryId(7L), HistoryAction.CLASSIFIED, "b", ts.plusDays(1), new RequestId(9L), new UserId(9L));

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void hashCodeIsConsistentWithEqualsWhenIdsAbsent() {
        var ts = LocalDateTime.of(2026, 4, 3, 8, 0);
        var reqId = new RequestId(2L);
        var actor = new UserId(3L);
        var a =
                new RequestHistory(null, HistoryAction.REGISTERED, "a", ts, reqId, actor);
        var b =
                new RequestHistory(null, HistoryAction.REGISTERED, "a", ts, reqId, actor);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
