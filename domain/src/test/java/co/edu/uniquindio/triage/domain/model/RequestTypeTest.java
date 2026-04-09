package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RequestTypeTest {

    @Test
    @DisplayName("Should create and update RequestType with trimming")
    void shouldCreateAndUpdateRequestType() {
        // Arrange
        RequestTypeId id = new RequestTypeId(1L);
        
        // Act
        RequestType type = new RequestType(id, "  Sustentación Extemporánea  ", "Desc", true);

        // Assert
        assertEquals("Sustentación Extemporánea", type.getName());
        assertEquals("Desc", type.getDescription());
        assertTrue(type.isActive());
        
        // Update
        type.updateName("  Novedad de Matrícula  ");
        assertEquals("Novedad de Matrícula", type.getName());
        
        type.deactivate();
        assertFalse(type.isActive());
        
        type.activate();
        assertTrue(type.isActive());
    }

    @Test
    @DisplayName("Should throw exception for invalid names")
    void shouldThrowExceptionForInvalidNames() {
        RequestTypeId id = new RequestTypeId(1L);
        assertThrows(IllegalArgumentException.class, () -> new RequestType(id, null, "Desc", true));
        assertThrows(IllegalArgumentException.class, () -> new RequestType(id, "   ", "Desc", true));
        assertThrows(IllegalArgumentException.class, () -> new RequestType(id, "a".repeat(101), "Desc", true));
    }

    @Test
    @DisplayName("Should validate equality based on ID")
    void shouldValidateEquality() {
        RequestTypeId id1 = new RequestTypeId(1L);
        RequestTypeId id2 = new RequestTypeId(1L);
        RequestTypeId id3 = new RequestTypeId(2L);
        
        RequestType t1 = new RequestType(id1, "Name 1", "Desc", true);
        RequestType t2 = new RequestType(id2, "Name 2", "Desc", false);
        RequestType t3 = new RequestType(id3, "Name 1", "Desc", true);

        assertEquals(t1, t2);
        assertNotEquals(t1, t3);
        assertEquals(t1.hashCode(), t2.hashCode());
    }
}
