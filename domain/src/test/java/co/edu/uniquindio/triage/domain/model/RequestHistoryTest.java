package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.model.id.RequestHistoryId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RequestHistoryTest {

    @Test
    @DisplayName("Should create RequestHistory and maintain integrity")
    void shouldCreateRequestHistory() {
        // Arrange
        RequestHistoryId id = new RequestHistoryId(1L);
        RequestId requestId = new RequestId(10L);
        UserId performedById = new UserId(5L);
        UserId responsibleId = new UserId(6L);
        LocalDateTime now = LocalDateTime.now();
        
        // Act
        RequestHistory history = new RequestHistory(id, HistoryAction.CLASSIFIED, "Classified manually", now, requestId, performedById, responsibleId);

        // Assert
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
        
        assertThrows(IllegalArgumentException.class, 
            () -> new RequestHistory(id, HistoryAction.REGISTERED, longObs, LocalDateTime.now(), requestId, performedById));
    }
}
