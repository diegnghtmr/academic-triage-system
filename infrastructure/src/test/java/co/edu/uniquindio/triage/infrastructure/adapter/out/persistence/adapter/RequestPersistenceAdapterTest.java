package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;
import co.edu.uniquindio.triage.application.port.out.persistence.RequestSearchCriteria;
import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.CatalogPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.RequestPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.OriginChannelJpaRepository;
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
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = RequestPersistenceAdapterTest.TestApplication.class)
@Import(co.edu.uniquindio.triage.infrastructure.config.PersistenceConfiguration.class)
class RequestPersistenceAdapterTest {

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
    private RequestTypeJpaRepository requestTypeJpaRepository;

    @Autowired
    private OriginChannelJpaRepository originChannelJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private EntityManager entityManager;

    private RequestPersistenceAdapter requestPersistenceAdapter;
    private RequestIdPersistenceAdapter requestIdPersistenceAdapter;
    private CatalogPersistenceAdapter catalogPersistenceAdapter;

    @BeforeEach
    void setUp() {
        var userMapper = new UserPersistenceMapper();
        var catalogMapper = Mappers.getMapper(CatalogPersistenceMapper.class);
        var requestMapper = new RequestPersistenceMapper(userMapper, catalogMapper);
        requestPersistenceAdapter = new RequestPersistenceAdapter(requestJpaRepository, requestMapper, entityManager);
        requestIdPersistenceAdapter = new RequestIdPersistenceAdapter(requestJpaRepository);
        catalogPersistenceAdapter = new CatalogPersistenceAdapter(requestTypeJpaRepository, originChannelJpaRepository, catalogMapper);
    }

    @Test
    void nextIdSaveAndLoadMustPreserveAggregateConsistency() {
        var requester = saveUser("student-a", Role.STUDENT);
        var requestType = saveRequestType("Cupo batch2");
        var originChannel = saveOriginChannel("Correo batch2");
        var nextId = requestIdPersistenceAdapter.nextId();

        var request = new AcademicRequest(
                nextId,
                "Necesito un cupo adicional para la materia de arquitectura",
                requester.getId(),
                originChannel.getId(),
                requestType.getId(),
                LocalDate.of(2026, 4, 10),
                false,
                LocalDateTime.of(2026, 3, 23, 10, 30)
        );

        requestPersistenceAdapter.save(request);
        entityManager.flush();
        entityManager.clear();

        var loaded = requestPersistenceAdapter.loadById(nextId);

        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().getId()).isEqualTo(nextId);
        assertThat(loaded.orElseThrow().getApplicantId()).isEqualTo(requester.getId());
        assertThat(loaded.orElseThrow().getOriginChannelId()).isEqualTo(originChannel.getId());
        assertThat(loaded.orElseThrow().getRequestTypeId()).isEqualTo(requestType.getId());
        assertThat(loaded.orElseThrow().getHistory()).hasSize(1);
        assertThat(loaded.orElseThrow().getHistory().getFirst().getAction()).isEqualTo(HistoryAction.REGISTERED);
        assertThat(loaded.orElseThrow().getHistory().getFirst().getRequestId()).isEqualTo(nextId);
    }

    @Test
    void detailLoadMustExposeRequesterAssignedToAndOrderedHistory() {
        var requester = saveUser("student-b", Role.STUDENT);
        var staff = saveUser("staff-b", Role.STAFF);
        var requestType = saveRequestType("Reintegro batch2");
        var originChannel = saveOriginChannel("Sistema web batch2");

        var reservedId = requestIdPersistenceAdapter.nextId();
        var request = AcademicRequest.reconstitute(
                reservedId,
                "Solicito revisión prioritaria del reintegro académico",
                RequestStatus.IN_PROGRESS,
                Priority.HIGH,
                "Regla aplicada",
                LocalDate.of(2026, 4, 15),
                LocalDateTime.of(2026, 3, 20, 8, 0),
                false,
                null,
                null,
                null,
                null,
                requester.getId(),
                staff.getId(),
                originChannel.getId(),
                requestType.getId(),
                java.util.List.of(),
                java.util.List.of(
                        new co.edu.uniquindio.triage.domain.model.RequestHistory(
                                null,
                                HistoryAction.ASSIGNED,
                                "Asignada a staff",
                                LocalDateTime.of(2026, 3, 20, 9, 0),
                                reservedId,
                                staff.getId()
                        ),
                        new co.edu.uniquindio.triage.domain.model.RequestHistory(
                                null,
                                HistoryAction.REGISTERED,
                                "Registro inicial",
                                LocalDateTime.of(2026, 3, 20, 8, 0),
                                reservedId,
                                requester.getId()
                        )
                )
        );

        requestPersistenceAdapter.save(request);
        entityManager.flush();
        entityManager.clear();

        var detail = requestPersistenceAdapter.loadDetailById(reservedId);

        assertThat(detail).isPresent();
        assertThat(detail.orElseThrow().requester().getId()).isEqualTo(requester.getId());
        assertThat(detail.orElseThrow().assignedTo()).isPresent();
        assertThat(detail.orElseThrow().assignedTo().orElseThrow().getId()).isEqualTo(staff.getId());
        assertThat(detail.orElseThrow().history()).hasSize(2);
        assertThat(detail.orElseThrow().history().get(0).historyEntry().getAction()).isEqualTo(HistoryAction.REGISTERED);
        assertThat(detail.orElseThrow().history().get(1).historyEntry().getAction()).isEqualTo(HistoryAction.ASSIGNED);
    }

    @Test
    void searchMustApplyRequesterFilterAssignedToFilterAndSortByRegistrationDate() {
        var studentA = saveUser("student-c", Role.STUDENT);
        var studentB = saveUser("student-d", Role.STUDENT);
        var staff = saveUser("staff-c", Role.STAFF);
        var requestType = saveRequestType("Homologación batch2");
        var originChannel = saveOriginChannel("Ventanilla batch2");

        var firstId = requestIdPersistenceAdapter.nextId();
        requestPersistenceAdapter.save(AcademicRequest.reconstitute(
                firstId,
                "Primera solicitud del estudiante A",
                RequestStatus.IN_PROGRESS,
                Priority.MEDIUM,
                "Pendiente de revisión",
                null,
                LocalDateTime.of(2026, 3, 10, 9, 0),
                false,
                null,
                null,
                null,
                null,
                studentA.getId(),
                staff.getId(),
                originChannel.getId(),
                requestType.getId(),
                java.util.List.of(),
                java.util.List.of()
        ));
        var secondId = requestIdPersistenceAdapter.nextId();
        requestPersistenceAdapter.save(AcademicRequest.reconstitute(
                secondId,
                "Solicitud más reciente del estudiante A",
                RequestStatus.IN_PROGRESS,
                Priority.MEDIUM,
                "Pendiente de revisión",
                null,
                LocalDateTime.of(2026, 3, 12, 9, 0),
                false,
                null,
                null,
                null,
                null,
                studentA.getId(),
                staff.getId(),
                originChannel.getId(),
                requestType.getId(),
                java.util.List.of(),
                java.util.List.of()
        ));
        var thirdId = requestIdPersistenceAdapter.nextId();
        requestPersistenceAdapter.save(AcademicRequest.reconstitute(
                thirdId,
                "Solicitud de otro estudiante",
                RequestStatus.IN_PROGRESS,
                Priority.MEDIUM,
                "Pendiente de revisión",
                null,
                LocalDateTime.of(2026, 3, 14, 9, 0),
                false,
                null,
                null,
                null,
                null,
                studentB.getId(),
                staff.getId(),
                originChannel.getId(),
                requestType.getId(),
                java.util.List.of(),
                java.util.List.of()
        ));
        entityManager.flush();
        entityManager.clear();

        var page = requestPersistenceAdapter.search(new RequestSearchCriteria(
                Optional.of(RequestStatus.IN_PROGRESS),
                Optional.empty(),
                Optional.of(Priority.MEDIUM),
                Optional.of(staff.getId()),
                Optional.of(studentA.getId()),
                Optional.of(LocalDate.of(2026, 3, 1)),
                Optional.of(LocalDate.of(2026, 3, 31)),
                0,
                10,
                "registrationDateTime,desc"
        ));

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).extracting(summary -> summary.request().getId().value())
                .containsExactly(secondId.value(), firstId.value());
        assertThat(page.content()).extracting(RequestSummary::requester)
                .allMatch(user -> user.getId().equals(studentA.getId()));
        assertThat(page.content()).extracting(summary -> summary.assignedTo().orElseThrow().getId())
                .allMatch(id -> id.equals(staff.getId()));
    }

    @Test
    void searchMustApplyRequestTypeInclusiveDateBoundsAndPaginationMetadata() {
        var requester = saveUser("student-e", Role.STUDENT);
        var staff = saveUser("staff-d", Role.STAFF);
        var matchingType = saveRequestType("Cancelación batch2");
        var otherType = saveRequestType("Otro tipo batch2");
        var originChannel = saveOriginChannel("Portal batch2");

        var ignoredByDate = persistRequest(
                requester,
                staff,
                matchingType,
                originChannel,
                "Fuera de rango inferior",
                LocalDateTime.of(2026, 3, 9, 23, 59),
                RequestStatus.REGISTERED,
                Priority.LOW
        );
        var firstMatching = persistRequest(
                requester,
                staff,
                matchingType,
                originChannel,
                "Primer match",
                LocalDateTime.of(2026, 3, 10, 0, 0),
                RequestStatus.REGISTERED,
                Priority.LOW
        );
        var ignoredByType = persistRequest(
                requester,
                staff,
                otherType,
                originChannel,
                "Tipo distinto",
                LocalDateTime.of(2026, 3, 11, 10, 0),
                RequestStatus.REGISTERED,
                Priority.LOW
        );
        var secondMatching = persistRequest(
                requester,
                staff,
                matchingType,
                originChannel,
                "Segundo match",
                LocalDateTime.of(2026, 3, 12, 23, 59),
                RequestStatus.REGISTERED,
                Priority.LOW
        );
        var ignoredByUpperDate = persistRequest(
                requester,
                staff,
                matchingType,
                originChannel,
                "Fuera de rango superior",
                LocalDateTime.of(2026, 3, 13, 0, 0),
                RequestStatus.REGISTERED,
                Priority.LOW
        );
        entityManager.flush();
        entityManager.clear();

        var firstPage = requestPersistenceAdapter.search(new RequestSearchCriteria(
                Optional.of(RequestStatus.REGISTERED),
                Optional.of(matchingType.getId()),
                Optional.of(Priority.LOW),
                Optional.of(staff.getId()),
                Optional.of(requester.getId()),
                Optional.of(LocalDate.of(2026, 3, 10)),
                Optional.of(LocalDate.of(2026, 3, 12)),
                0,
                1,
                "registrationDateTime,asc"
        ));
        var secondPage = requestPersistenceAdapter.search(new RequestSearchCriteria(
                Optional.of(RequestStatus.REGISTERED),
                Optional.of(matchingType.getId()),
                Optional.of(Priority.LOW),
                Optional.of(staff.getId()),
                Optional.of(requester.getId()),
                Optional.of(LocalDate.of(2026, 3, 10)),
                Optional.of(LocalDate.of(2026, 3, 12)),
                1,
                1,
                "registrationDateTime,asc"
        ));

        assertThat(ignoredByDate).isNotNull();
        assertThat(ignoredByType).isNotNull();
        assertThat(ignoredByUpperDate).isNotNull();
        assertThat(firstPage.totalElements()).isEqualTo(2);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.currentPage()).isEqualTo(0);
        assertThat(firstPage.pageSize()).isEqualTo(1);
        assertThat(firstPage.content()).extracting(summary -> summary.request().getId().value())
                .containsExactly(firstMatching.value());
        assertThat(secondPage.totalElements()).isEqualTo(2);
        assertThat(secondPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.currentPage()).isEqualTo(1);
        assertThat(secondPage.pageSize()).isEqualTo(1);
        assertThat(secondPage.content()).extracting(summary -> summary.request().getId().value())
                .containsExactly(secondMatching.value());
    }

    @Test
    void searchMustUseIdAsSecondarySortForStablePagingWhenRegistrationDateMatches() {
        var requester = saveUser("student-f", Role.STUDENT);
        var staff = saveUser("staff-e", Role.STAFF);
        var requestType = saveRequestType("Estabilidad batch2");
        var originChannel = saveOriginChannel("Formulario batch2");
        var sharedTimestamp = LocalDateTime.of(2026, 3, 18, 14, 0);

        var firstId = persistRequest(
                requester,
                staff,
                requestType,
                originChannel,
                "Primera con misma fecha",
                sharedTimestamp,
                RequestStatus.CLASSIFIED,
                Priority.HIGH
        );
        var secondId = persistRequest(
                requester,
                staff,
                requestType,
                originChannel,
                "Segunda con misma fecha",
                sharedTimestamp,
                RequestStatus.CLASSIFIED,
                Priority.HIGH
        );
        entityManager.flush();
        entityManager.clear();

        var page = requestPersistenceAdapter.search(new RequestSearchCriteria(
                Optional.of(RequestStatus.CLASSIFIED),
                Optional.of(requestType.getId()),
                Optional.of(Priority.HIGH),
                Optional.of(staff.getId()),
                Optional.of(requester.getId()),
                Optional.empty(),
                Optional.empty(),
                0,
                2,
                "registrationDateTime,desc"
        ));

        assertThat(page.content()).extracting(summary -> summary.request().getId().value())
                .containsExactly(secondId.value(), firstId.value());
    }

    @Test
    void catalogLookupMustReturnCanonicalDomainObjects() {
        var requestType = saveRequestType("Certificado batch2");
        var originChannel = saveOriginChannel("Teléfono batch2");

        var loadedRequestType = catalogPersistenceAdapter.loadById(requestType.getId());
        var loadedOriginChannel = catalogPersistenceAdapter.loadById(originChannel.getId());

        assertThat(loadedRequestType).isPresent();
        assertThat(loadedRequestType.orElseThrow().getName()).isEqualTo("Certificado batch2");
        assertThat(loadedOriginChannel).isPresent();
        assertThat(loadedOriginChannel.orElseThrow().getName()).isEqualTo("Teléfono batch2");
    }

    private User saveUser(String username, Role role) {
        var entity = new UserJpaEntity(
                null,
                username,
                username + "@uniquindio.edu.co",
                "ID-" + username,
                "Nombre",
                "Apellido",
                role.name(),
                true,
                "hashed-password"
        );
        var saved = userJpaRepository.saveAndFlush(entity);
        return User.reconstitute(
                new UserId(saved.getId()),
                new Username(saved.getUsername()),
                saved.getFirstName(),
                saved.getLastName(),
                new PasswordHash(saved.getPasswordHash()),
                new Identification(saved.getIdentification()),
                new Email(saved.getEmail()),
                role,
                saved.isActive()
        );
    }

    private RequestType saveRequestType(String name) {
        var entity = new co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity();
        entity.setName(name);
        entity.setDescription(name + " description");
        entity.setActive(true);
        var saved = requestTypeJpaRepository.saveAndFlush(entity);
        return new RequestType(new RequestTypeId(saved.getId()), saved.getName(), saved.getDescription(), saved.isActive());
    }

    private OriginChannel saveOriginChannel(String name) {
        var entity = new co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.OriginChannelJpaEntity();
        entity.setName(name);
        entity.setActive(true);
        var saved = originChannelJpaRepository.saveAndFlush(entity);
        return new OriginChannel(new OriginChannelId(saved.getId()), saved.getName(), saved.isActive());
    }

    private RequestId persistRequest(User requester,
                                     User assignedTo,
                                     RequestType requestType,
                                     OriginChannel originChannel,
                                     String description,
                                     LocalDateTime registrationDateTime,
                                     RequestStatus status,
                                     Priority priority) {
        var requestId = requestIdPersistenceAdapter.nextId();
        requestPersistenceAdapter.save(AcademicRequest.reconstitute(
                requestId,
                description,
                status,
                priority,
                "Justificación " + description,
                null,
                registrationDateTime,
                false,
                null,
                null,
                null,
                null,
                requester.getId(),
                assignedTo == null ? null : assignedTo.getId(),
                originChannel.getId(),
                requestType.getId(),
                java.util.List.of(),
                java.util.List.of()
        ));
        return requestId;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
