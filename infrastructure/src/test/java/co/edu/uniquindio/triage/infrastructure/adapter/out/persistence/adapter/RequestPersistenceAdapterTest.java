package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;
import co.edu.uniquindio.triage.application.port.out.persistence.NewAcademicRequest;
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
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.CatalogPersistenceMapper;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    private CatalogPersistenceAdapter catalogPersistenceAdapter;

    @BeforeEach
    void setUp() {
        var userMapper = new UserPersistenceMapper();
        var catalogMapper = Mappers.getMapper(CatalogPersistenceMapper.class);
        var requestMapper = new RequestPersistenceMapper(userMapper, catalogMapper);
        requestPersistenceAdapter = new RequestPersistenceAdapter(requestJpaRepository, requestMapper, entityManager);
        catalogPersistenceAdapter = new CatalogPersistenceAdapter(requestTypeJpaRepository, originChannelJpaRepository, catalogMapper);
    }

    @Test
    void createMustPersistAndLoadAggregateWithDatabaseGeneratedId() {
        var requester = saveUser("student-a", Role.STUDENT);
        var requestType = saveRequestType("Cupo batch2");
        var originChannel = saveOriginChannel("Correo batch2");

        var request = requestPersistenceAdapter.create(new NewAcademicRequest(
                "Necesito un cupo adicional para la materia de arquitectura",
                requester.getId().orElseThrow(),
                originChannel.getId(),
                requestType.getId(),
                LocalDate.of(2026, 4, 10),
                false,
                LocalDateTime.of(2026, 3, 23, 10, 30)
        ));
        entityManager.flush();
        entityManager.clear();

        var loaded = requestPersistenceAdapter.loadById(request.getId());

        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().getId()).isEqualTo(request.getId());
        assertThat(loaded.orElseThrow().getApplicantId()).isEqualTo(requester.getId().orElseThrow());
        assertThat(loaded.orElseThrow().getOriginChannelId()).isEqualTo(originChannel.getId());
        assertThat(loaded.orElseThrow().getRequestTypeId()).isEqualTo(requestType.getId());
        assertThat(loaded.orElseThrow().getHistory()).hasSize(1);
        assertThat(loaded.orElseThrow().getHistory().getFirst().getAction()).isEqualTo(HistoryAction.REGISTERED);
        assertThat(loaded.orElseThrow().getHistory().getFirst().getRequestId()).isEqualTo(request.getId());
    }

    @Test
    void detailLoadMustExposeRequesterAssignedToAndOrderedHistory() {
        var requester = saveUser("student-b", Role.STUDENT);
        var staff = saveUser("staff-b", Role.STAFF);
        var requestType = saveRequestType("Reintegro batch2");
        var originChannel = saveOriginChannel("Sistema web batch2");

        var request = createRegisteredRequest(
                requester,
                originChannel,
                requestType,
                "Solicito revisión prioritaria del reintegro académico",
                LocalDate.of(2026, 4, 15),
                LocalDateTime.of(2026, 3, 20, 8, 0),
                false
        );
        request.classify(requestType.getId(), "Registro inicial", requester.getId().orElseThrow(), LocalDateTime.of(2026, 3, 20, 8, 30));
        request.prioritize(Priority.HIGH, "Regla aplicada", requester.getId().orElseThrow(), LocalDateTime.of(2026, 3, 20, 8, 45));
        request.assign(staff, staff.getId().orElseThrow(), "Asignada a staff", LocalDateTime.of(2026, 3, 20, 9, 0));

        requestPersistenceAdapter.save(request);
        entityManager.flush();
        entityManager.clear();

        var detail = requestPersistenceAdapter.loadDetailById(request.getId());

        assertThat(detail).isPresent();
        assertThat(detail.orElseThrow().requester().getId().orElseThrow()).isEqualTo(requester.getId().orElseThrow());
        assertThat(detail.orElseThrow().assignedTo()).isPresent();
        assertThat(detail.orElseThrow().assignedTo().orElseThrow().getId().orElseThrow()).isEqualTo(staff.getId().orElseThrow());
        assertThat(detail.orElseThrow().history()).hasSize(4);
        assertThat(detail.orElseThrow().history().get(0).historyEntry().getAction()).isEqualTo(HistoryAction.REGISTERED);
        assertThat(detail.orElseThrow().history().get(1).historyEntry().getAction()).isEqualTo(HistoryAction.CLASSIFIED);
        assertThat(detail.orElseThrow().history().get(2).historyEntry().getAction()).isEqualTo(HistoryAction.PRIORITIZED);
        assertThat(detail.orElseThrow().history().get(3).historyEntry().getAction()).isEqualTo(HistoryAction.ASSIGNED);
    }

    @Test
    void searchMustApplyRequesterFilterAssignedToFilterAndSortByRegistrationDate() {
        var studentA = saveUser("student-c", Role.STUDENT);
        var studentB = saveUser("student-d", Role.STUDENT);
        var staff = saveUser("staff-c", Role.STAFF);
        var requestType = saveRequestType("Homologación batch2");
        var originChannel = saveOriginChannel("Ventanilla batch2");

        var firstId = persistRequest(
                studentA,
                staff,
                requestType,
                originChannel,
                "Primera solicitud del estudiante A",
                LocalDateTime.of(2026, 3, 10, 9, 0),
                RequestStatus.IN_PROGRESS,
                Priority.MEDIUM
        );
        var secondId = persistRequest(
                studentA,
                staff,
                requestType,
                originChannel,
                "Solicitud más reciente del estudiante A",
                LocalDateTime.of(2026, 3, 12, 9, 0),
                RequestStatus.IN_PROGRESS,
                Priority.MEDIUM
        );
        var thirdId = persistRequest(
                studentB,
                staff,
                requestType,
                originChannel,
                "Solicitud de otro estudiante",
                LocalDateTime.of(2026, 3, 14, 9, 0),
                RequestStatus.IN_PROGRESS,
                Priority.MEDIUM
        );
        entityManager.flush();
        entityManager.clear();

        var page = requestPersistenceAdapter.search(new RequestSearchCriteria(
                Optional.of(RequestStatus.IN_PROGRESS),
                Optional.empty(),
                Optional.of(Priority.MEDIUM),
                Optional.of(staff.getId().orElseThrow()),
                Optional.of(studentA.getId().orElseThrow()),
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
                .allMatch(user -> user.getId().orElseThrow().equals(studentA.getId().orElseThrow()));
        assertThat(page.content()).extracting(summary -> summary.assignedTo().orElseThrow().getId().orElseThrow())
                .allMatch(id -> id.equals(staff.getId().orElseThrow()));
    }

    @Test
    void triageUpdateMustPersistAppendedHistoryResponsibleAuditAndStateChanges() {
        var requester = saveUser("triage-stu", Role.STUDENT);
        var triageActor = saveUser("triage-act", Role.STAFF);
        var assignee = saveUser("triage-asg", Role.STAFF);
        var initialType = saveRequestType("Tipo inicial triage");
        var classifiedType = saveRequestType("Tipo clasificado triage");
        var originChannel = saveOriginChannel("Canal triage");
        var request = createRegisteredRequest(
                requester,
                originChannel,
                initialType,
                "Solicitud para validar persistencia de triage",
                LocalDate.of(2026, 4, 20),
                LocalDateTime.of(2026, 3, 24, 8, 0),
                false
        );
        request.classify(
                classifiedType.getId(),
                "Clasificación manual",
                triageActor.getId().orElseThrow(),
                LocalDateTime.of(2026, 3, 24, 9, 0)
        );
        request.prioritize(
                Priority.HIGH,
                "Urgencia académica validada",
                triageActor.getId().orElseThrow(),
                LocalDateTime.of(2026, 3, 24, 9, 15)
        );
        request.assign(
                assignee,
                triageActor.getId().orElseThrow(),
                "Asignada al staff responsable",
                LocalDateTime.of(2026, 3, 24, 9, 30)
        );

        assertThat(request.getHistory()).hasSize(4);
        assertThat(request.getHistory()).extracting(history -> history.getId()).containsNull();

        requestPersistenceAdapter.save(request);
        entityManager.flush();
        entityManager.clear();

        var loaded = requestPersistenceAdapter.loadById(request.getId()).orElseThrow();
        var detail = requestPersistenceAdapter.loadDetailById(request.getId()).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(RequestStatus.IN_PROGRESS);
        assertThat(loaded.getRequestTypeId()).isEqualTo(classifiedType.getId());
        assertThat(loaded.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(loaded.getPriorityJustification()).isEqualTo("Urgencia académica validada");
        assertThat(loaded.getResponsibleId()).isEqualTo(assignee.getId().orElseThrow());
        assertThat(loaded.getHistory()).hasSize(4);
        assertThat(loaded.getHistory()).allMatch(history -> history.getId() != null);
        assertThat(loaded.getHistory()).extracting(history -> history.getAction())
                .containsExactly(
                        HistoryAction.REGISTERED,
                        HistoryAction.CLASSIFIED,
                        HistoryAction.PRIORITIZED,
                        HistoryAction.ASSIGNED
                );

        var assignedHistory = loaded.getHistory().get(3);
        assertThat(assignedHistory.getPerformedById()).isEqualTo(triageActor.getId().orElseThrow());
        assertThat(assignedHistory.getResponsibleId()).isEqualTo(assignee.getId().orElseThrow());

        assertThat(detail.assignedTo()).isPresent();
        assertThat(detail.assignedTo().orElseThrow().getId().orElseThrow()).isEqualTo(assignee.getId().orElseThrow());
        assertThat(detail.history()).hasSize(4);
        assertThat(detail.history().get(3).performedBy().getId().orElseThrow()).isEqualTo(triageActor.getId().orElseThrow());
        assertThat(detail.history().get(3).historyEntry().getResponsibleId()).isEqualTo(assignee.getId().orElseThrow());
    }

    @Test
    void attendMustPersistAttendanceObservationAndAppendedHistory() {
        var requester = saveUser("attend-stu", Role.STUDENT);
        var triageActor = saveUser("attend-act", Role.STAFF);
        var assignee = saveUser("attend-asg", Role.STAFF);
        var initialType = saveRequestType("Tipo inicial attend");
        var classifiedType = saveRequestType("Tipo clasificado attend");
        var originChannel = saveOriginChannel("Canal attend");
        var request = createRegisteredRequest(
                requester,
                originChannel,
                initialType,
                "Solicitud para validar persistencia de atención",
                LocalDate.of(2026, 4, 22),
                LocalDateTime.of(2026, 3, 24, 8, 0),
                false
        );
        request.classify(
                classifiedType.getId(),
                "Clasificación para atención",
                triageActor.getId().orElseThrow(),
                LocalDateTime.of(2026, 3, 24, 9, 0)
        );
        request.prioritize(
                Priority.HIGH,
                "Urgencia confirmada para atención",
                triageActor.getId().orElseThrow(),
                LocalDateTime.of(2026, 3, 24, 9, 15)
        );
        request.assign(
                assignee,
                triageActor.getId().orElseThrow(),
                "Asignada para atención",
                LocalDateTime.of(2026, 3, 24, 9, 30)
        );
        requestPersistenceAdapter.save(request);
        entityManager.flush();
        entityManager.clear();

        var inProgressRequest = requestPersistenceAdapter.loadById(request.getId()).orElseThrow();
        var attendanceObservation = "Atención manual completada con validación documental";
        inProgressRequest.attend(attendanceObservation, assignee.getId().orElseThrow(), LocalDateTime.of(2026, 3, 24, 10, 0));

        requestPersistenceAdapter.save(inProgressRequest);
        entityManager.flush();
        entityManager.clear();

        var loaded = requestPersistenceAdapter.loadById(request.getId()).orElseThrow();
        var detail = requestPersistenceAdapter.loadDetailById(request.getId()).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(RequestStatus.ATTENDED);
        assertThat(loaded.getAttendanceObservation()).isEqualTo(attendanceObservation);
        assertThat(loaded.getHistory()).hasSize(5);
        assertThat(loaded.getHistory()).allMatch(history -> history.getId() != null);
        assertThat(loaded.getHistory()).extracting(history -> history.getAction())
                .containsExactly(
                        HistoryAction.REGISTERED,
                        HistoryAction.CLASSIFIED,
                        HistoryAction.PRIORITIZED,
                        HistoryAction.ASSIGNED,
                        HistoryAction.ATTENDED
                );
        assertThat(loaded.getHistory().getLast().getObservations()).isEqualTo(attendanceObservation);
        assertThat(loaded.getHistory().getLast().getPerformedById()).isEqualTo(assignee.getId().orElseThrow());

        assertThat(detail.request().getAttendanceObservation()).isEqualTo(attendanceObservation);
        assertThat(detail.history()).hasSize(5);
        assertThat(detail.history().getLast().historyEntry().getAction()).isEqualTo(HistoryAction.ATTENDED);
        assertThat(detail.history().getLast().historyEntry().getObservations()).isEqualTo(attendanceObservation);
        assertThat(detail.history().getLast().performedBy().getId().orElseThrow()).isEqualTo(assignee.getId().orElseThrow());
    }

    @Test
    void closeMustPersistFullTwoThousandCharacterObservationWithoutTruncation() {
        var requester = saveUser("close-stu", Role.STUDENT);
        var staff = saveUser("close-staff", Role.STAFF);
        var requestType = saveRequestType("Cierre extenso");
        var originChannel = saveOriginChannel("Mesa de ayuda");
        var request = createRegisteredRequest(
                requester,
                originChannel,
                requestType,
                "Solicitud para validar el cierre con observación extensa",
                LocalDate.of(2026, 4, 25),
                LocalDateTime.of(2026, 3, 24, 8, 0),
                false
        );
        request.classify(requestType.getId(), "Clasificación para cierre", staff.getId().orElseThrow(), LocalDateTime.of(2026, 3, 24, 8, 30));
        request.prioritize(Priority.MEDIUM, "Priorizada para cierre", staff.getId().orElseThrow(), LocalDateTime.of(2026, 3, 24, 8, 45));
        request.assign(staff, staff.getId().orElseThrow(), "Asignada para cierre", LocalDateTime.of(2026, 3, 24, 9, 0));
        request.attend("Atendida previamente", staff.getId().orElseThrow(), LocalDateTime.of(2026, 3, 24, 9, 30));
        var closingObservation = "c".repeat(2000);

        request.close(closingObservation, staff.getId().orElseThrow(), LocalDateTime.of(2026, 3, 24, 12, 0));

        requestPersistenceAdapter.save(request);
        entityManager.flush();
        entityManager.clear();

        var loaded = requestPersistenceAdapter.loadById(request.getId()).orElseThrow();
        var detail = requestPersistenceAdapter.loadDetailById(request.getId()).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(RequestStatus.CLOSED);
        assertThat(loaded.getClosingObservation()).hasSize(2000);
        assertThat(loaded.getClosingObservation()).isEqualTo(closingObservation);
        assertThat(loaded.getHistory()).extracting(history -> history.getAction())
                .contains(HistoryAction.CLOSED);
        assertThat(loaded.getHistory().getLast().getObservations()).isEqualTo(closingObservation);

        assertThat(detail.request().getStatus()).isEqualTo(RequestStatus.CLOSED);
        assertThat(detail.request().getClosingObservation()).hasSize(2000);
        assertThat(detail.request().getClosingObservation()).isEqualTo(closingObservation);
        assertThat(detail.history().getLast().historyEntry().getAction()).isEqualTo(HistoryAction.CLOSED);
        assertThat(detail.history().getLast().historyEntry().getObservations()).isEqualTo(closingObservation);
    }

    @Test
    void cancelMustPersistFullTwoThousandCharacterReasonAndTerminalHistory() {
        var requester = saveUser("cancel-stu", Role.STUDENT);
        var staff = saveUser("cancel-staff", Role.STAFF);
        var requestType = saveRequestType("Cancelación extensa");
        var originChannel = saveOriginChannel("Portal cancelación");
        var request = createRegisteredRequest(
                requester,
                originChannel,
                requestType,
                "Solicitud para validar cancelación persistida",
                LocalDate.of(2026, 4, 26),
                LocalDateTime.of(2026, 3, 24, 8, 0),
                false
        );
        request.classify(requestType.getId(), "Clasificada para eventual cancelación", staff.getId().orElseThrow(), LocalDateTime.of(2026, 3, 24, 8, 30));
        request.prioritize(Priority.MEDIUM, "Clasificada para eventual cancelación", staff.getId().orElseThrow(), LocalDateTime.of(2026, 3, 24, 8, 45));
        var cancellationReason = "x".repeat(2000);

        request.cancel(cancellationReason, staff.getId().orElseThrow(), LocalDateTime.of(2026, 3, 24, 12, 30));

        requestPersistenceAdapter.save(request);
        entityManager.flush();
        entityManager.clear();

        var loaded = requestPersistenceAdapter.loadById(request.getId()).orElseThrow();
        var detail = requestPersistenceAdapter.loadDetailById(request.getId()).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(loaded.getCancellationReason()).hasSize(2000);
        assertThat(loaded.getCancellationReason()).isEqualTo(cancellationReason);
        assertThat(loaded.getHistory().getLast().getAction()).isEqualTo(HistoryAction.CANCELLED);
        assertThat(loaded.getHistory().getLast().getObservations()).isEqualTo(cancellationReason);
        assertThat(loaded.getHistory().getLast().getPerformedById()).isEqualTo(staff.getId().orElseThrow());

        assertThat(detail.request().getStatus()).isEqualTo(RequestStatus.CANCELLED);
        assertThat(detail.request().getCancellationReason()).hasSize(2000);
        assertThat(detail.request().getCancellationReason()).isEqualTo(cancellationReason);
        assertThat(detail.history().getLast().historyEntry().getAction()).isEqualTo(HistoryAction.CANCELLED);
        assertThat(detail.history().getLast().historyEntry().getObservations()).isEqualTo(cancellationReason);
        assertThat(detail.history().getLast().performedBy().getId().orElseThrow()).isEqualTo(staff.getId().orElseThrow());
    }

    @Test
    void rejectMustPersistFullTwoThousandCharacterReasonAndTerminalHistory() {
        var requester = saveUser("reject-stu", Role.STUDENT);
        var admin = saveUser("reject-admin", Role.ADMIN);
        var requestType = saveRequestType("Rechazo extenso");
        var originChannel = saveOriginChannel("Portal rechazo");
        var request = createRegisteredRequest(
                requester,
                originChannel,
                requestType,
                "Solicitud para validar rechazo persistido",
                LocalDate.of(2026, 4, 27),
                LocalDateTime.of(2026, 3, 24, 8, 0),
                false
        );
        var rejectionReason = "r".repeat(2000);

        request.reject(rejectionReason, admin.getId().orElseThrow(), LocalDateTime.of(2026, 3, 24, 13, 0));

        requestPersistenceAdapter.save(request);
        entityManager.flush();
        entityManager.clear();

        var loaded = requestPersistenceAdapter.loadById(request.getId()).orElseThrow();
        var detail = requestPersistenceAdapter.loadDetailById(request.getId()).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(loaded.getRejectionReason()).hasSize(2000);
        assertThat(loaded.getRejectionReason()).isEqualTo(rejectionReason);
        assertThat(loaded.getHistory().getLast().getAction()).isEqualTo(HistoryAction.REJECTED);
        assertThat(loaded.getHistory().getLast().getObservations()).isEqualTo(rejectionReason);
        assertThat(loaded.getHistory().getLast().getPerformedById()).isEqualTo(admin.getId().orElseThrow());

        assertThat(detail.request().getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(detail.request().getRejectionReason()).hasSize(2000);
        assertThat(detail.request().getRejectionReason()).isEqualTo(rejectionReason);
        assertThat(detail.history().getLast().historyEntry().getAction()).isEqualTo(HistoryAction.REJECTED);
        assertThat(detail.history().getLast().historyEntry().getObservations()).isEqualTo(rejectionReason);
        assertThat(detail.history().getLast().performedBy().getId().orElseThrow()).isEqualTo(admin.getId().orElseThrow());
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
                RequestStatus.CLASSIFIED,
                Priority.LOW
        );
        var firstMatching = persistRequest(
                requester,
                staff,
                matchingType,
                originChannel,
                "Primer match",
                LocalDateTime.of(2026, 3, 10, 0, 0),
                RequestStatus.CLASSIFIED,
                Priority.LOW
        );
        var ignoredByType = persistRequest(
                requester,
                staff,
                otherType,
                originChannel,
                "Tipo distinto",
                LocalDateTime.of(2026, 3, 11, 10, 0),
                RequestStatus.CLASSIFIED,
                Priority.LOW
        );
        var secondMatching = persistRequest(
                requester,
                staff,
                matchingType,
                originChannel,
                "Segundo match",
                LocalDateTime.of(2026, 3, 12, 23, 59),
                RequestStatus.CLASSIFIED,
                Priority.LOW
        );
        var ignoredByUpperDate = persistRequest(
                requester,
                staff,
                matchingType,
                originChannel,
                "Fuera de rango superior",
                LocalDateTime.of(2026, 3, 13, 0, 0),
                RequestStatus.CLASSIFIED,
                Priority.LOW
        );
        entityManager.flush();
        entityManager.clear();

        var firstPage = requestPersistenceAdapter.search(new RequestSearchCriteria(
                Optional.of(RequestStatus.CLASSIFIED),
                Optional.of(matchingType.getId()),
                Optional.of(Priority.LOW),
                Optional.empty(),
                Optional.of(requester.getId().orElseThrow()),
                Optional.of(LocalDate.of(2026, 3, 10)),
                Optional.of(LocalDate.of(2026, 3, 12)),
                0,
                1,
                "registrationDateTime,asc"
        ));
        var secondPage = requestPersistenceAdapter.search(new RequestSearchCriteria(
                Optional.of(RequestStatus.CLASSIFIED),
                Optional.of(matchingType.getId()),
                Optional.of(Priority.LOW),
                Optional.empty(),
                Optional.of(requester.getId().orElseThrow()),
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
                Optional.empty(),
                Optional.of(requester.getId().orElseThrow()),
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

    @Test
    void saveMustRejectUpdatingNonExistentRequest() {
        var orphan = AcademicRequest.reconstitute(
                new RequestId(9_999_888_777L),
                "Descripción mínima de diez caracteres aquí",
                RequestStatus.REGISTERED,
                null,
                null,
                LocalDate.of(2026, 4, 20),
                LocalDateTime.of(2026, 4, 10, 8, 0),
                false,
                null,
                null,
                null,
                null,
                new UserId(1L),
                null,
                new OriginChannelId(1L),
                new RequestTypeId(1L),
                List.of(),
                List.of());

        assertThatThrownBy(() -> requestPersistenceAdapter.save(orphan))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inexistente");
    }

    @Test
    void searchMustRejectSortExpressionWithoutCommaSeparatedFieldAndDirection() {
        assertThatThrownBy(() -> requestPersistenceAdapter.search(new RequestSearchCriteria(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0,
                10,
                "soloCampoSinDirección"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sort");
    }

    @Test
    void searchMustRejectUnsupportedSortField() {
        assertThatThrownBy(() -> requestPersistenceAdapter.search(new RequestSearchCriteria(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0,
                10,
                "campoInventado,asc"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("soportado");
    }

    @Test
    void searchMustRejectInvalidSortDirection() {
        assertThatThrownBy(() -> requestPersistenceAdapter.search(new RequestSearchCriteria(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0,
                10,
                "registrationDateTime,diagonal"
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dirección");
    }

    @Test
    void appliedRuleIdsMustRoundTripThroughPersistence() {
        var requester = saveUser("student-rules-rt", Role.STUDENT);
        var requestType = saveRequestType("Tipo rules RT");
        var originChannel = saveOriginChannel("Canal rules RT");
        var request = requestPersistenceAdapter.create(new NewAcademicRequest(
                "Descripción suficiente para crear la solicitud académica",
                requester.getId().orElseThrow(),
                originChannel.getId(),
                requestType.getId(),
                LocalDate.of(2026, 5, 1),
                false,
                LocalDateTime.of(2026, 4, 2, 9, 0)
        ));
        request.applyRule(new BusinessRuleId(1L));
        request.applyRule(new BusinessRuleId(2L));
        requestPersistenceAdapter.save(request);
        entityManager.flush();
        entityManager.clear();

        var loaded = requestPersistenceAdapter.loadById(request.getId()).orElseThrow();
        assertThat(loaded.getAppliedRuleIds())
                .extracting(BusinessRuleId::value)
                .containsExactlyInAnyOrder(1L, 2L);
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
                "hashed-password",
                null
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
        var request = createRegisteredRequest(requester, originChannel, requestType, description, null, registrationDateTime, false);
        if (status != RequestStatus.REGISTERED) {
            request.classify(requestType.getId(), "Clasificación " + description, assignedTo == null ? requester.getId().orElseThrow() : assignedTo.getId().orElseThrow(), registrationDateTime.plusMinutes(1));
        }
        if (priority != null) {
            request.prioritize(priority, "Justificación " + description, assignedTo == null ? requester.getId().orElseThrow() : assignedTo.getId().orElseThrow(), registrationDateTime.plusMinutes(2));
        }
        if (status == RequestStatus.IN_PROGRESS && assignedTo != null) {
            request.assign(assignedTo, assignedTo.getId().orElseThrow(), "Asignación " + description, registrationDateTime.plusMinutes(3));
        }
        requestPersistenceAdapter.save(request);
        return request.getId();
    }

    private AcademicRequest createRegisteredRequest(User requester,
                                                    OriginChannel originChannel,
                                                    RequestType requestType,
                                                    String description,
                                                    LocalDate deadline,
                                                    LocalDateTime registrationDateTime,
                                                    boolean aiSuggested) {
        return requestPersistenceAdapter.create(new NewAcademicRequest(
                description,
                requester.getId().orElseThrow(),
                originChannel.getId(),
                requestType.getId(),
                deadline,
                aiSuggested,
                registrationDateTime
        ));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
