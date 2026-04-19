package co.edu.uniquindio.triage.infrastructure.idempotency;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.idempotency.TransactionalIdempotencyExecutor;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.IdempotencyKeyJpaRepository;
import co.edu.uniquindio.triage.infrastructure.config.PersistenceConfiguration;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.MariaDBContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = TransactionalIdempotencyExecutorTest.TestApplication.class)
@Import(PersistenceConfiguration.class)
class TransactionalIdempotencyExecutorTest {

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
    private IdempotencyKeyJpaRepository idempotencyKeyJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TransactionalIdempotencyExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new TransactionalIdempotencyExecutor(
                idempotencyKeyJpaRepository,
                new ObjectMapper().findAndRegisterModules(),
                jdbcTemplate,
                transactionManager,
                new SimpleMeterRegistry(),
                new IdempotencyProperties()
        );
    }

    @Test
    void executeMustProcessOnceAndReplayExactStoredResponse() {
        var request = new IdempotencyRequest("requests:create", "user-42", "idem-001", "fp-001");
        var invocations = new AtomicInteger();

        var first = executor.execute(request, () -> {
            invocations.incrementAndGet();
            return new IdempotencyResponseEnvelope(
                    201,
                    "application/json",
                    Map.of("ETag", List.of("\"7\""), "Location", List.of("/api/v1/requests/7")),
                    "{\"id\":7}"
            );
        });

        var second = executor.execute(request, () -> {
            invocations.incrementAndGet();
            return new IdempotencyResponseEnvelope(500, "application/json", Map.of(), "{}");
        });

        assertThat(first.status()).isEqualTo(IdempotencyExecutionResult.Status.PROCESSED);
        assertThat(second.status()).isEqualTo(IdempotencyExecutionResult.Status.REPLAY);
        assertThat(invocations).hasValue(1);
        assertThat(second.response()).contains(first.response().orElseThrow());

        var stored = idempotencyKeyJpaRepository
                .findByScopeAndPrincipalScopeAndIdempotencyKey("requests:create", "user-42", "idem-001")
                .orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(TransactionalIdempotencyExecutor.IdempotencyStatus.COMPLETED.name());
        assertThat(stored.getResponseStatusCode()).isEqualTo(201);
        assertThat(stored.getResponseBody()).isEqualTo("{\"id\":7}");
    }

    @Test
    void claimMustReturnMismatchWhenKeyWasUsedWithDifferentFingerprint() {
        var first = executor.claim(new IdempotencyRequest("requests:create", "", "idem-002", "fp-001"));
        var second = executor.claim(new IdempotencyRequest("requests:create", "", "idem-002", "fp-002"));

        assertThat(first.status()).isEqualTo(TransactionalIdempotencyExecutor.ClaimStatus.CLAIMED);
        assertThat(second.status()).isEqualTo(TransactionalIdempotencyExecutor.ClaimStatus.MISMATCH);
    }

    @Test
    void claimMustReturnOutstandingWhileSameFingerprintIsStillProcessing() {
        var first = executor.claim(new IdempotencyRequest("requests:create", "", "idem-003", "fp-003"));
        var second = executor.claim(new IdempotencyRequest("requests:create", "", "idem-003", "fp-003"));

        assertThat(first.status()).isEqualTo(TransactionalIdempotencyExecutor.ClaimStatus.CLAIMED);
        assertThat(second.status()).isEqualTo(TransactionalIdempotencyExecutor.ClaimStatus.OUTSTANDING);
    }

    @Test
    void claimMustSetExpiresAtAndLastSeenAt() {
        var beforeClaim = LocalDateTime.now().minusSeconds(1);
        var request = new IdempotencyRequest("requests:create", "user-ttl", "idem-ttl-001", "fp-ttl-001");

        executor.execute(request, () -> new IdempotencyResponseEnvelope(201, "application/json", Map.of(), "{}"));

        var stored = idempotencyKeyJpaRepository
                .findByScopeAndPrincipalScopeAndIdempotencyKey("requests:create", "user-ttl", "idem-ttl-001")
                .orElseThrow();

        assertThat(stored.getExpiresAt())
                .isNotNull()
                .isAfter(beforeClaim.plusDays(6));
        assertThat(stored.getLastSeenAt())
                .isNotNull()
                .isAfter(beforeClaim);
    }

    @Test
    void replayMustRefreshLastSeenAt() {
        var request = new IdempotencyRequest("requests:create", "user-ttl", "idem-ttl-002", "fp-ttl-002");

        executor.execute(request, () -> new IdempotencyResponseEnvelope(201, "application/json", Map.of(), "{}"));

        var afterFirst = idempotencyKeyJpaRepository
                .findByScopeAndPrincipalScopeAndIdempotencyKey("requests:create", "user-ttl", "idem-ttl-002")
                .orElseThrow()
                .getLastSeenAt();

        executor.execute(request, () -> new IdempotencyResponseEnvelope(500, "application/json", Map.of(), "{}"));

        var afterReplay = idempotencyKeyJpaRepository
                .findByScopeAndPrincipalScopeAndIdempotencyKey("requests:create", "user-ttl", "idem-ttl-002")
                .orElseThrow()
                .getLastSeenAt();

        assertThat(afterReplay)
                .isNotNull()
                .isAfterOrEqualTo(afterFirst);
    }

    @Test
    void aiScopeMustGetShorterTtlThanDefault() {
        var requestAi = new IdempotencyRequest("ai:suggest-classification", "user-ttl", "idem-ai-001", "fp-ai-001");
        var requestBusiness = new IdempotencyRequest("requests:create", "user-ttl", "idem-biz-001", "fp-biz-001");

        executor.execute(requestAi, () -> new IdempotencyResponseEnvelope(200, "application/json", Map.of(), "{}"));
        executor.execute(requestBusiness, () -> new IdempotencyResponseEnvelope(201, "application/json", Map.of(), "{}"));

        var aiStored = idempotencyKeyJpaRepository
                .findByScopeAndPrincipalScopeAndIdempotencyKey("ai:suggest-classification", "user-ttl", "idem-ai-001")
                .orElseThrow();
        var bizStored = idempotencyKeyJpaRepository
                .findByScopeAndPrincipalScopeAndIdempotencyKey("requests:create", "user-ttl", "idem-biz-001")
                .orElseThrow();

        assertThat(aiStored.getExpiresAt()).isBefore(bizStored.getExpiresAt());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
