package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.out.persistence.NewAcademicRequest;
import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.OriginChannelJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.CatalogPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.OriginChannelJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestTypeJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import co.edu.uniquindio.triage.infrastructure.config.PersistenceConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MariaDBContainer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that concurrent mutations on the same AcademicRequest aggregate
 * are serialized by the PESSIMISTIC_WRITE lock in loadByIdForMutation().
 *
 * Covered scenarios:
 * - Two concurrent addInternalNote calls: both entries must be preserved (no lost update).
 * - Two concurrent prioritize calls on a CLASSIFIED request: both PRIORITIZED entries must be
 *   preserved — the second thread loads the post-commit state from the first, preventing a
 *   silent overwrite of the history.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration,classpath:db/migration-dev"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = RequestAggregateConcurrencyIntegrationTest.TestApplication.class)
@Import(PersistenceConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RequestAggregateConcurrencyIntegrationTest {

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

    @Autowired
    private PlatformTransactionManager transactionManager;

    private RequestPersistenceAdapter adapter;
    private TransactionTemplate txTemplate;

    private Long requestId;
    private UserId performerA;
    private UserId performerB;

    @BeforeEach
    void setUp() {
        var userMapper = new UserPersistenceMapper();
        var catalogMapper = Mappers.getMapper(CatalogPersistenceMapper.class);
        var requestMapper = new RequestPersistenceMapper(userMapper, catalogMapper);
        adapter = new RequestPersistenceAdapter(requestJpaRepository, requestMapper, entityManager);
        txTemplate = new TransactionTemplate(transactionManager);

        txTemplate.execute(status -> {
            var user = persistUser("student-concurrency-" + System.nanoTime());
            var staffA = persistUser("staff-concurrency-a-" + System.nanoTime());
            var staffB = persistUser("staff-concurrency-b-" + System.nanoTime());
            var requestType = persistRequestType("Type-concurrency-" + System.nanoTime());
            var originChannel = persistOriginChannel("Channel-concurrency-" + System.nanoTime());

            performerA = new UserId(staffA.getId());
            performerB = new UserId(staffB.getId());

            var created = adapter.create(new NewAcademicRequest(
                    "Solicitud para test de concurrencia del agregado",
                    new UserId(user.getId()),
                    new OriginChannelId(originChannel.getId()),
                    new RequestTypeId(requestType.getId()),
                    LocalDate.of(2026, 12, 31),
                    false,
                    LocalDateTime.now()
            ));
            requestId = created.getId().value();
            return null;
        });
    }

    @Test
    void concurrentInternalNotesMustBothBePreservedViaPessimisticLock() throws Exception {
        var errors = new CopyOnWriteArrayList<Throwable>();
        var barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        var futureA = CompletableFuture.runAsync(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                txTemplate.execute(status -> {
                    var request = adapter.loadByIdForMutation(new RequestId(requestId)).orElseThrow();
                    request.addInternalNote("Note A - thread 1", performerA, LocalDateTime.now());
                    adapter.save(request);
                    return null;
                });
            } catch (Throwable t) {
                errors.add(t);
            }
        }, executor);

        var futureB = CompletableFuture.runAsync(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                txTemplate.execute(status -> {
                    var request = adapter.loadByIdForMutation(new RequestId(requestId)).orElseThrow();
                    request.addInternalNote("Note B - thread 2", performerB, LocalDateTime.now());
                    adapter.save(request);
                    return null;
                });
            } catch (Throwable t) {
                errors.add(t);
            }
        }, executor);

        CompletableFuture.allOf(futureA, futureB).get(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(errors)
                .as("No exceptions expected from concurrent threads")
                .isEmpty();

        // Both notes must exist — the second thread loaded the fresh state after the first committed
        var history = txTemplate.execute(status ->
                adapter.loadById(new RequestId(requestId)).orElseThrow().getHistory()
        );

        assertThat(history).isNotNull();
        var internalNotes = history.stream()
                .filter(h -> h.getAction() == HistoryAction.INTERNAL_NOTE)
                .map(h -> h.getObservations())
                .toList();

        assertThat(internalNotes)
                .as("Both notes must be preserved — no lost update")
                .hasSize(2)
                .containsExactlyInAnyOrder("Note A - thread 1", "Note B - thread 2");
    }

    @Test
    void concurrentPrioritizeMustBothBePreservedViaPessimisticLock() throws Exception {
        // Prerequisite: move the request to CLASSIFIED so prioritize() is a valid domain operation.
        // prioritize() does NOT change RequestStatus — it stays CLASSIFIED — so the second thread
        // can also call prioritize() after the first commits. The PESSIMISTIC_WRITE lock ensures
        // Thread B loads the fresh post-commit state and adds its own PRIORITIZED entry without
        // overwriting Thread A's. Without the lock, the second save() would silently drop Thread A's
        // history entry (lost update).
        txTemplate.execute(status -> {
            var request = adapter.loadByIdForMutation(new RequestId(requestId)).orElseThrow();
            request.classify(request.getRequestTypeId(), "Classified for concurrent prioritize test", performerA, LocalDateTime.now());
            adapter.save(request);
            return null;
        });

        var errors = new CopyOnWriteArrayList<Throwable>();
        var barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        var futureA = CompletableFuture.runAsync(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                txTemplate.execute(status -> {
                    var request = adapter.loadByIdForMutation(new RequestId(requestId)).orElseThrow();
                    request.prioritize(Priority.HIGH, "High priority — urgent academic situation", performerA, LocalDateTime.now());
                    adapter.save(request);
                    return null;
                });
            } catch (Throwable t) {
                errors.add(t);
            }
        }, executor);

        var futureB = CompletableFuture.runAsync(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                txTemplate.execute(status -> {
                    var request = adapter.loadByIdForMutation(new RequestId(requestId)).orElseThrow();
                    request.prioritize(Priority.LOW, "Low priority — non-urgent academic situation", performerB, LocalDateTime.now());
                    adapter.save(request);
                    return null;
                });
            } catch (Throwable t) {
                errors.add(t);
            }
        }, executor);

        CompletableFuture.allOf(futureA, futureB).get(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(errors)
                .as("No exceptions expected from concurrent prioritize threads")
                .isEmpty();

        var history = txTemplate.execute(status ->
                adapter.loadById(new RequestId(requestId)).orElseThrow().getHistory()
        );

        var prioritizedEntries = history.stream()
                .filter(h -> h.getAction() == HistoryAction.PRIORITIZED)
                .toList();

        // Both PRIORITIZED entries must exist — Thread B saw Thread A's committed state
        // and added its own entry. No lost update, no silent overwrite.
        assertThat(prioritizedEntries)
                .as("Both prioritize operations must be preserved — no lost update via pessimistic lock")
                .hasSize(2);
    }

    @Test
    void loadByIdForMutationMustAcquireLockAndReturnFullHistoryForExistingRequest() {
        var loaded = txTemplate.execute(status ->
                adapter.loadByIdForMutation(new RequestId(requestId))
        );

        assertThat(loaded).isPresent();
        assertThat(loaded.orElseThrow().getId().value()).isEqualTo(requestId);
        // History is loaded eagerly (REGISTERED entry from creation)
        assertThat(loaded.orElseThrow().getHistory())
                .as("History must be loaded with the locked aggregate")
                .hasSize(1);
        assertThat(loaded.orElseThrow().getHistory().getFirst().getAction())
                .isEqualTo(HistoryAction.REGISTERED);
    }

    // --- Helpers ---

    private UserJpaEntity persistUser(String username) {
        var user = new UserJpaEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        // identification must be unique — extract the trailing nanotime digits from the username
        String digits = username.replaceAll("[^0-9]", "");
        user.setIdentification(digits.length() > 15 ? digits.substring(digits.length() - 15) : digits);
        user.setFirstName("Test");
        user.setLastName(username.length() > 50 ? username.substring(0, 50) : username);
        user.setPasswordHash("hashed");
        user.setRole(Role.STAFF.name());
        user.setActive(true);
        return userJpaRepository.saveAndFlush(user);
    }

    private RequestTypeJpaEntity persistRequestType(String name) {
        var entity = new RequestTypeJpaEntity();
        entity.setName(name);
        entity.setDescription("Test type");
        entity.setActive(true);
        entity.setVersion(0L);
        return requestTypeJpaRepository.saveAndFlush(entity);
    }

    private OriginChannelJpaEntity persistOriginChannel(String name) {
        var entity = new OriginChannelJpaEntity();
        entity.setName(name);
        entity.setActive(true);
        entity.setVersion(0L);
        return originChannelJpaRepository.saveAndFlush(entity);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
