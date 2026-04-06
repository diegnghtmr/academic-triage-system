package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.out.persistence.DashboardMetricsCriteria;
import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.AcademicRequestJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.OriginChannelJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestHistoryJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.OriginChannelJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestHistoryJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestTypeJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = ReportMetricsPersistenceAdapterTest.TestApplication.class)
@Import(co.edu.uniquindio.triage.infrastructure.config.PersistenceConfiguration.class)
class ReportMetricsPersistenceAdapterTest {

    private static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11");

    static {
        MARIADB.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
    }

    @Autowired
    private RequestJpaRepository requestJpaRepository;

    @Autowired
    private RequestHistoryJpaRepository requestHistoryJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private RequestTypeJpaRepository requestTypeJpaRepository;

    @Autowired
    private OriginChannelJpaRepository originChannelJpaRepository;

    @Autowired
    private EntityManager entityManager;

    private ReportMetricsPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ReportMetricsPersistenceAdapter(requestJpaRepository, requestHistoryJpaRepository, userJpaRepository);
    }

    @Test
    void load_ShouldCalculateMetricsCorrectly() {
        // Arrange
        var staff = saveUser("reporter-staff", Role.STAFF);
        var student = saveUser("reporter-student", Role.STUDENT);
        var type = saveRequestType("Tipo Reporte");
        var channel = saveOriginChannel("Canal Reporte");

        // Request 1: Closed, registered today
        var r1 = saveRequest("R1", student, type, channel, LocalDateTime.now().minusHours(2));
        addHistory(r1, HistoryAction.CLOSED, LocalDateTime.now().minusHours(1), staff, null);

        // Request 2: Registered today, not closed
        saveRequest("R2", student, type, channel, LocalDateTime.now().minusMinutes(30));

        entityManager.flush();
        entityManager.clear();

        var criteria = new DashboardMetricsCriteria(
            Optional.of(LocalDateTime.now().minusDays(1)),
            Optional.of(LocalDateTime.now().plusDays(1))
        );

        // Act
        var metrics = adapter.load(criteria);

        // Assert
        assertThat(metrics.totalRequests()).isEqualTo(2);
        assertThat(metrics.requestsByStatus().get(RequestStatus.CLOSED)).isEqualTo(1);
        assertThat(metrics.averageResolutionTimeHours()).isGreaterThan(0.0);
        assertThat(metrics.topResponsibles()).hasSize(1);
        assertThat(metrics.topResponsibles().get(0).username()).isEqualTo("reporter-staff");
        assertThat(metrics.topResponsibles().get(0).closedRequestsCount()).isEqualTo(1);
        assertThat(metrics.topResponsibles().get(0).role()).isEqualTo(Role.STAFF);
    }

    @Test
    void load_WhenNoClosures_ShouldReturnZeroAverage() {
        // Arrange
        var criteria = new DashboardMetricsCriteria(Optional.empty(), Optional.empty());

        // Act
        var metrics = adapter.load(criteria);

        // Assert
        assertThat(metrics.averageResolutionTimeHours()).isEqualTo(0.0);
        assertThat(metrics.topResponsibles()).isEmpty();
    }

    @Test
    void load_ShouldIncludeResponsiblesFromPerformedByWhenResponsibleIdIsNull() {
        // Arrange
        var staff = saveUser("closer-only", Role.STAFF);
        var student = saveUser("stu-1", Role.STUDENT);
        var type = saveRequestType("Type A");
        var channel = saveOriginChannel("Web");

        var r1 = saveRequest("R3", student, type, channel, LocalDateTime.now().minusHours(5));
        // Action CLOSED with performedBy = staff, but responsible_id = null (simulating AcademicRequest.close() behavior)
        addHistory(r1, HistoryAction.CLOSED, LocalDateTime.now().minusHours(4), staff, null);

        entityManager.flush();
        entityManager.clear();

        var criteria = new DashboardMetricsCriteria(Optional.empty(), Optional.empty());

        // Act
        var metrics = adapter.load(criteria);

        // Assert
        assertThat(metrics.topResponsibles()).hasSize(1);
        assertThat(metrics.topResponsibles().get(0).username()).isEqualTo("closer-only");
        assertThat(metrics.topResponsibles().get(0).closedRequestsCount()).isEqualTo(1);
    }

    private UserJpaEntity saveUser(String username, Role role) {
        var user = new UserJpaEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setIdentification("ID-" + username);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setRole(role.name());
        user.setActive(true);
        user.setPasswordHash("hash");
        return userJpaRepository.saveAndFlush(user);
    }

    private RequestTypeJpaEntity saveRequestType(String name) {
        var type = new RequestTypeJpaEntity();
        type.setName(name);
        type.setActive(true);
        return requestTypeJpaRepository.saveAndFlush(type);
    }

    private OriginChannelJpaEntity saveOriginChannel(String name) {
        var channel = new OriginChannelJpaEntity();
        channel.setName(name);
        channel.setActive(true);
        return originChannelJpaRepository.saveAndFlush(channel);
    }

    private AcademicRequestJpaEntity saveRequest(String desc, UserJpaEntity student, RequestTypeJpaEntity type, OriginChannelJpaEntity channel, LocalDateTime reg) {
        var request = new AcademicRequestJpaEntity();
        request.setDescription(desc);
        request.setApplicant(student);
        request.setRequestType(type);
        request.setOriginChannel(channel);
        request.setRegistrationDateTime(reg);
        request.setStatus(RequestStatus.REGISTERED.name());
        request.setPriority(Priority.MEDIUM.name());
        request.setAiSuggested(false);
        return requestJpaRepository.saveAndFlush(request);
    }

    private void addHistory(AcademicRequestJpaEntity request, HistoryAction action, LocalDateTime ts, UserJpaEntity performedBy, UserJpaEntity responsible) {
        var history = new RequestHistoryJpaEntity();
        history.setRequest(request);
        history.setAction(action.name());
        history.setTimestamp(ts);
        history.setPerformedBy(performedBy);
        history.setResponsible(responsible);
        history.setObservations("Some observations");
        requestHistoryJpaRepository.saveAndFlush(history);
        
        // Update request status if it's a state-changing action
        if (action == HistoryAction.CLOSED) {
            request.setStatus(RequestStatus.CLOSED.name());
            requestJpaRepository.saveAndFlush(request);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
