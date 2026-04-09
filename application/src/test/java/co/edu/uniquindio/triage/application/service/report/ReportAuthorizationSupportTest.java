package co.edu.uniquindio.triage.application.service.report;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportAuthorizationSupportTest {

    private final ReportAuthorizationSupport authSupport = new ReportAuthorizationSupport();

    @Test
    @DisplayName("Should allow ADMIN for ensureAdmin")
    void shouldAllowAdmin() {
        AuthenticatedActor admin = new AuthenticatedActor(new UserId(1L), "admin", Role.ADMIN);
        assertDoesNotThrow(() -> authSupport.ensureAdmin(admin));
    }

    @Test
    @DisplayName("Should throw for STAFF or STUDENT in ensureAdmin")
    void shouldThrowForNonAdmin() {
        AuthenticatedActor staff = new AuthenticatedActor(new UserId(1L), "staff", Role.STAFF);
        AuthenticatedActor student = new AuthenticatedActor(new UserId(2L), "student", Role.STUDENT);
        
        assertThrows(UnauthorizedOperationException.class, () -> authSupport.ensureAdmin(staff));
        assertThrows(UnauthorizedOperationException.class, () -> authSupport.ensureAdmin(student));
    }

    @Test
    @DisplayName("Should throw NullPointerException for null actor")
    void shouldThrowForNullActor() {
        assertThrows(NullPointerException.class, () -> authSupport.ensureAdmin(null));
    }
}
