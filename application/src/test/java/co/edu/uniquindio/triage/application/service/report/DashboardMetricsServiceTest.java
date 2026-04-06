package co.edu.uniquindio.triage.application.service.report;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.report.GetDashboardMetricsQueryModel;
import co.edu.uniquindio.triage.application.port.in.report.DashboardMetricsView;
import co.edu.uniquindio.triage.application.port.out.persistence.DashboardMetricsCriteria;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadDashboardMetricsPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardMetricsServiceTest {

    @Mock
    private LoadDashboardMetricsPort loadDashboardMetricsPort;

    private DashboardMetricsService dashboardMetricsService;

    @BeforeEach
    void setUp() {
        dashboardMetricsService = new DashboardMetricsService(
            loadDashboardMetricsPort,
            new ReportAuthorizationSupport()
        );
    }

    @Test
    void execute_WhenAdmin_ShouldLoadMetrics() {
        // Arrange
        var actor = new AuthenticatedActor(new UserId(1L), "admin", Role.ADMIN);
        var dateFrom = LocalDate.of(2026, 1, 1);
        var dateTo = LocalDate.of(2026, 1, 31);
        var query = new GetDashboardMetricsQueryModel(Optional.of(dateFrom), Optional.of(dateTo));

        var expectedView = new DashboardMetricsView(10, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), 2.5, Collections.emptyList());
        when(loadDashboardMetricsPort.load(any())).thenReturn(expectedView);

        // Act
        DashboardMetricsView result = dashboardMetricsService.execute(query, actor);

        // Assert
        assertNotNull(result);
        assertEquals(expectedView, result);

        var captor = ArgumentCaptor.forClass(DashboardMetricsCriteria.class);
        verify(loadDashboardMetricsPort).load(captor.capture());
        
        DashboardMetricsCriteria criteria = captor.getValue();
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), criteria.from().get());
        assertEquals(LocalDateTime.of(2026, 2, 1, 0, 0), criteria.toExclusive().get());
    }

    @Test
    void execute_WhenStaff_ShouldThrowUnauthorized() {
        // Arrange
        var actor = new AuthenticatedActor(new UserId(2L), "staff", Role.STAFF);
        var query = new GetDashboardMetricsQueryModel(Optional.empty(), Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedOperationException.class, () -> dashboardMetricsService.execute(query, actor));
        verifyNoInteractions(loadDashboardMetricsPort);
    }

    @Test
    void execute_WhenStudent_ShouldThrowUnauthorized() {
        // Arrange
        var actor = new AuthenticatedActor(new UserId(3L), "student", Role.STUDENT);
        var query = new GetDashboardMetricsQueryModel(Optional.empty(), Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedOperationException.class, () -> dashboardMetricsService.execute(query, actor));
        verifyNoInteractions(loadDashboardMetricsPort);
    }
}
