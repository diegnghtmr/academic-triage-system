package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.report.DashboardMetricsView;
import co.edu.uniquindio.triage.application.port.in.report.GetDashboardMetricsQuery;
import co.edu.uniquindio.triage.application.port.in.report.TopResponsibleMetric;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.ReportRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.security.AuthenticatedUser;
import co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration;
import co.edu.uniquindio.triage.infrastructure.testsupport.NoopLoadUserAuthPortTestConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@ContextConfiguration(classes = {
        ReportController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        NoopLoadUserAuthPortTestConfiguration.class,
        ReportControllerTest.TestMappersConfiguration.class,
        ReportControllerTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        NoopLoadUserAuthPortTestConfiguration.class,
        ReportControllerTest.TestMappersConfiguration.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GetDashboardMetricsQuery getDashboardMetricsQuery;

    @Test
    void getDashboardMetrics_WhenAdmin_ShouldReturn200() throws Exception {
        // Arrange
        var view = new DashboardMetricsView(
            100L,
            Map.of(RequestStatus.CLOSED, 60L, RequestStatus.REGISTERED, 40L),
            Map.of("Cupo", 50L),
            Map.of(Priority.HIGH, 30L),
            12.5,
            List.of(new TopResponsibleMetric(new UserId(10L), "staff1", "Juan", "Perez", "123", "juan@u.co", Role.STAFF, true, 15L))
        );
        given(getDashboardMetricsQuery.execute(any(), any())).willReturn(view);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/reports/dashboard")
                .with(adminAuthentication())
                .param("dateFrom", "2026-01-01")
                .param("dateTo", "2026-01-31")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRequests").value(100))
            .andExpect(jsonPath("$.requestsByStatus.CLOSED").value(60))
            .andExpect(jsonPath("$.averageResolutionTimeHours").value(12.5))
            .andExpect(jsonPath("$.topResponsibles[0].user.username").value("staff1"))
            .andExpect(jsonPath("$.topResponsibles[0].resolvedCount").value(15));
    }

    @Test
    void getDashboardMetrics_WhenStaff_ShouldReturn403() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/reports/dashboard")
                .with(staffAuthentication())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void getDashboardMetrics_WhenUnauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/reports/dashboard")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    private RequestPostProcessor adminAuthentication() {
        return authentication(99L, "admin", Role.ADMIN);
    }

    private RequestPostProcessor staffAuthentication() {
        return authentication(10L, "staff01", Role.STAFF);
    }

    private RequestPostProcessor authentication(long id, String username, Role role) {
        var principal = new AuthenticatedUser(id, username, role, true);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    @TestConfiguration
    static class TestMappersConfiguration {
        @Bean
        UserRestMapper userRestMapper() {
            return new UserRestMapper();
        }

        @Bean
        ReportRestMapper reportRestMapper(UserRestMapper userRestMapper) {
            return new ReportRestMapper(userRestMapper);
        }

        @Bean
        AuthenticatedActorMapper authenticatedActorMapper() {
            return new AuthenticatedActorMapper();
        }

        @Bean
        GetDashboardMetricsQuery getDashboardMetricsQuery() {
            return Mockito.mock(GetDashboardMetricsQuery.class);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
