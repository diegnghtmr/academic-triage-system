package co.edu.uniquindio.triage.infrastructure.idempotency;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.idempotency.IdempotencyCleanupService;
import co.edu.uniquindio.triage.infrastructure.config.PersistenceConfiguration;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = IdempotencyCleanupServiceIT.TestApplication.class)
@Import(PersistenceConfiguration.class)
// Disable @DataJpaTest's auto-rollback transaction: the cleanup service uses REQUIRES_NEW,
// which would deadlock waiting for the test transaction's row locks if the outer tx is active.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class IdempotencyCleanupServiceIT {

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.update("DELETE FROM idempotency_keys");
    }

    @Test
    void cleanupMustDeleteOnlyExpiredRecords() {
        insertRecord("key-expired-1", LocalDateTime.now().minusDays(1));
        insertRecord("key-expired-2", LocalDateTime.now().minusHours(1));
        insertRecord("key-valid", LocalDateTime.now().plusDays(7));

        var props = propertiesWithBatchSize(100);
        var service = new IdempotencyCleanupService(jdbcTemplate, transactionManager, props, new SimpleMeterRegistry());

        service.runCleanupBatch();

        var remaining = jdbcTemplate.queryForList(
                "SELECT idempotency_key FROM idempotency_keys ORDER BY idempotency_key", String.class);
        assertThat(remaining).containsExactly("key-valid");
    }

    @Test
    void cleanupMustRespectBatchLimit() {
        for (int i = 1; i <= 5; i++) {
            insertRecord("key-expired-" + i, LocalDateTime.now().minusDays(i));
        }

        var props = propertiesWithBatchSize(3);
        var service = new IdempotencyCleanupService(jdbcTemplate, transactionManager, props, new SimpleMeterRegistry());

        service.runCleanupBatch();

        var count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM idempotency_keys", Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void cleanupMustIncrementMetrics() {
        insertRecord("key-exp-1", LocalDateTime.now().minusDays(1));
        insertRecord("key-exp-2", LocalDateTime.now().minusDays(2));

        var meterRegistry = new SimpleMeterRegistry();
        var props = propertiesWithBatchSize(100);
        var service = new IdempotencyCleanupService(jdbcTemplate, transactionManager, props, meterRegistry);

        service.runCleanupBatch();

        assertThat(meterRegistry.counter("idempotency.cleanup.runs.total").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("idempotency.cleanup.deleted.total").count()).isEqualTo(2.0);
        assertThat(meterRegistry.counter("idempotency.cleanup.failures.total").count()).isZero();
    }

    @Test
    void cleanupMustNotDeleteRecordsWithNullExpiresAt() {
        insertRecordWithNullExpiry("key-no-expiry");

        var props = propertiesWithBatchSize(100);
        var service = new IdempotencyCleanupService(jdbcTemplate, transactionManager, props, new SimpleMeterRegistry());

        service.runCleanupBatch();

        var count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM idempotency_keys", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    private void insertRecord(String key, LocalDateTime expiresAt) {
        jdbcTemplate.update("""
                INSERT INTO idempotency_keys
                  (scope, principal_scope, idempotency_key, fingerprint, status, expires_at, last_seen_at, created_at, updated_at)
                VALUES
                  (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "requests:create", "user-test", key, "fp-" + key, "COMPLETED",
                expiresAt, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());
    }

    private void insertRecordWithNullExpiry(String key) {
        jdbcTemplate.update("""
                INSERT INTO idempotency_keys
                  (scope, principal_scope, idempotency_key, fingerprint, status, created_at, updated_at)
                VALUES
                  (?, ?, ?, ?, ?, ?, ?)
                """,
                "requests:create", "user-test", key, "fp-" + key, "COMPLETED",
                LocalDateTime.now(), LocalDateTime.now());
    }

    private IdempotencyProperties propertiesWithBatchSize(int batchSize) {
        var props = new IdempotencyProperties();
        props.getCleanup().setBatchSize(batchSize);
        return props;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
