package co.edu.uniquindio.triage.application.service.ai;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiAuthorizationSupportTest {

    private final AiAuthorizationSupport authSupport = new AiAuthorizationSupport();

    @Test
    @DisplayName("Should allow STAFF for ensureStaff")
    void shouldAllowStaffForEnsureStaff() {
        AuthenticatedActor actor = new AuthenticatedActor(new UserId(1L), "staff", Role.STAFF);
        assertDoesNotThrow(() -> authSupport.ensureStaff(actor));
    }

    @Test
    @DisplayName("Should throw exception for STUDENT in ensureStaff")
    void shouldThrowForStudentInEnsureStaff() {
        AuthenticatedActor actor = new AuthenticatedActor(new UserId(1L), "student", Role.STUDENT);
        assertThrows(UnauthorizedOperationException.class, () -> authSupport.ensureStaff(actor));
    }

    @Test
    @DisplayName("Should allow STAFF or ADMIN for ensureStaffOrAdmin")
    void shouldAllowStaffOrAdmin() {
        AuthenticatedActor staff = new AuthenticatedActor(new UserId(1L), "staff", Role.STAFF);
        AuthenticatedActor admin = new AuthenticatedActor(new UserId(2L), "admin", Role.ADMIN);
        
        assertDoesNotThrow(() -> authSupport.ensureStaffOrAdmin(staff));
        assertDoesNotThrow(() -> authSupport.ensureStaffOrAdmin(admin));
    }

    @Test
    @DisplayName("Should throw for STUDENT in ensureStaffOrAdmin")
    void shouldThrowForStudentInEnsureStaffOrAdmin() {
        AuthenticatedActor actor = new AuthenticatedActor(new UserId(1L), "student", Role.STUDENT);
        assertThrows(UnauthorizedOperationException.class, () -> authSupport.ensureStaffOrAdmin(actor));
    }

    @Test
    @DisplayName("Should throw NullPointerException for null actor")
    void shouldThrowForNullActor() {
        assertThrows(NullPointerException.class, () -> authSupport.ensureStaff(null));
        assertThrows(NullPointerException.class, () -> authSupport.ensureStaffOrAdmin(null));
    }
}
