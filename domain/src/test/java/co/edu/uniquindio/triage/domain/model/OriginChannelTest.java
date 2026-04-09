package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OriginChannelTest {

    @Test
    @DisplayName("Should create and update OriginChannel with trimming")
    void shouldCreateAndUpdateOriginChannel() {
        // Arrange
        OriginChannelId id = new OriginChannelId(1L);
        
        // Act
        OriginChannel channel = new OriginChannel(id, "  Correo Electrónico  ", true);

        // Assert
        assertEquals("Correo Electrónico", channel.getName());
        assertTrue(channel.isActive());
        
        // Update
        channel.updateName("  SAC  ");
        assertEquals("SAC", channel.getName());
        
        channel.deactivate();
        assertFalse(channel.isActive());
        
        channel.activate();
        assertTrue(channel.isActive());
    }

    @Test
    @DisplayName("Should throw exception for invalid names")
    void shouldThrowExceptionForInvalidNames() {
        OriginChannelId id = new OriginChannelId(1L);
        assertThrows(IllegalArgumentException.class, () -> new OriginChannel(id, null, true));
        assertThrows(IllegalArgumentException.class, () -> new OriginChannel(id, "   ", true));
        assertThrows(IllegalArgumentException.class, () -> new OriginChannel(id, "a".repeat(150), true)); // Limit is 100
    }

    @Test
    @DisplayName("Should validate equality based on ID")
    void shouldValidateEquality() {
        OriginChannelId id1 = new OriginChannelId(1L);
        OriginChannelId id2 = new OriginChannelId(1L);
        OriginChannelId id3 = new OriginChannelId(2L);
        
        OriginChannel c1 = new OriginChannel(id1, "Name 1", true);
        OriginChannel c2 = new OriginChannel(id2, "Name 2", false);
        OriginChannel c3 = new OriginChannel(id3, "Name 1", true);

        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
        assertEquals(c1.hashCode(), c2.hashCode());
    }
}
