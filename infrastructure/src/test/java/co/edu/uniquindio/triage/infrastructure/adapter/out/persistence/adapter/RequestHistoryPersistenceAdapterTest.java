package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.model.RequestHistory;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.AcademicRequestJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.OriginChannelJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestHistoryJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.RequestHistoryPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.OriginChannelJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestHistoryJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestTypeJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = RequestHistoryPersistenceAdapterTest.TestApplication.class)
@Import(co.edu.uniquindio.triage.infrastructure.config.PersistenceConfiguration.class)
class RequestHistoryPersistenceAdapterTest {

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
    private RequestHistoryJpaRepository historyRepository;

    @Autowired
    private RequestJpaRepository requestRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private RequestTypeJpaRepository requestTypeRepository;

    @Autowired
    private OriginChannelJpaRepository channelRepository;

    private RequestHistoryPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RequestHistoryPersistenceAdapter(historyRepository, new RequestHistoryPersistenceMapper());
    }

    @Test
    @DisplayName("GIVEN histories for a request WHEN loadRequestHistory THEN returns histories ordered by timestamp desc")
    void loadRequestHistoryOrdered() {
        UserJpaEntity user = new UserJpaEntity(null, "user1", "email1@test.com", "12345", "First", "Last", "STUDENT", true, "hash");
        user = userRepository.save(user);

        RequestTypeJpaEntity type = new RequestTypeJpaEntity();
        type.setName("Type 1");
        type.setDescription("Desc");
        type = requestTypeRepository.save(type);

        OriginChannelJpaEntity channel = OriginChannelJpaEntity.builder()
                .name("Web")
                .active(true)
                .build();
        channel = channelRepository.save(channel);

        AcademicRequestJpaEntity request = AcademicRequestJpaEntity.builder()
                .description("Test request description")
                .status(RequestStatus.REGISTERED.name())
                .registrationDateTime(LocalDateTime.now().minusDays(1))
                .applicant(user)
                .requestType(type)
                .originChannel(channel)
                .build();
        request = requestRepository.save(request);

        LocalDateTime now = LocalDateTime.now();
        historyRepository.save(createHistory(request, user, HistoryAction.REGISTERED, now.minusHours(2)));
        historyRepository.save(createHistory(request, user, HistoryAction.INTERNAL_NOTE, now.minusHours(1)));

        List<RequestHistory> histories = adapter.loadRequestHistory(new RequestId(request.getId()));

        assertThat(histories).hasSize(2);
        assertThat(histories.get(0).getAction()).isEqualTo(HistoryAction.INTERNAL_NOTE);
        assertThat(histories.get(1).getAction()).isEqualTo(HistoryAction.REGISTERED);
    }

    private RequestHistoryJpaEntity createHistory(AcademicRequestJpaEntity request, UserJpaEntity user, HistoryAction action, LocalDateTime timestamp) {
        RequestHistoryJpaEntity entity = new RequestHistoryJpaEntity();
        entity.setRequest(request);
        entity.setPerformedBy(user);
        entity.setAction(action.name());
        entity.setObservations("Observation");
        entity.setTimestamp(timestamp);
        return entity;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
