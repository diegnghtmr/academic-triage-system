package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.idempotency;

import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
public class IdempotencyCleanupService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyCleanupService.class);

    // Subquery alias avoids MariaDB's "can't specify target table for update in FROM clause" restriction.
    private static final String DELETE_EXPIRED_SQL = """
            DELETE FROM idempotency_keys
            WHERE id IN (
                SELECT id FROM (
                    SELECT id FROM idempotency_keys
                    WHERE expires_at IS NOT NULL
                      AND expires_at < ?
                    ORDER BY expires_at
                    LIMIT ?
                ) AS expired_batch
            )
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate cleanupTransactionTemplate;
    private final int batchSize;

    private final Counter deletedTotal;
    private final Counter runsTotal;
    private final Counter failuresTotal;
    private final Timer cleanupDuration;

    public IdempotencyCleanupService(JdbcTemplate jdbcTemplate,
                                     PlatformTransactionManager transactionManager,
                                     IdempotencyProperties properties,
                                     MeterRegistry meterRegistry) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate no puede ser null");
        this.batchSize = properties.getCleanup().getBatchSize();

        this.cleanupTransactionTemplate = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager no puede ser null"));
        this.cleanupTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Objects.requireNonNull(meterRegistry, "meterRegistry no puede ser null");
        this.deletedTotal = Counter.builder("idempotency.cleanup.deleted.total")
                .description("Rows deleted from idempotency_keys by cleanup batches")
                .register(meterRegistry);
        this.runsTotal = Counter.builder("idempotency.cleanup.runs.total")
                .description("Total cleanup batch executions (success or failure)")
                .register(meterRegistry);
        this.failuresTotal = Counter.builder("idempotency.cleanup.failures.total")
                .description("Cleanup batch executions that ended in error")
                .register(meterRegistry);
        this.cleanupDuration = Timer.builder("idempotency.cleanup.duration")
                .description("Wall-clock time per cleanup batch execution")
                .register(meterRegistry);
    }

    public void runCleanupBatch() {
        runsTotal.increment();
        try {
            int deleted = cleanupDuration.record(this::executeDeleteBatch);
            deletedTotal.increment(deleted);
            log.debug("Idempotency cleanup batch deleted {} expired records", deleted);
        } catch (Exception e) {
            failuresTotal.increment();
            log.error("Idempotency cleanup batch failed", e);
        }
    }

    private int executeDeleteBatch() {
        var deadline = LocalDateTime.now();
        Integer deleted = cleanupTransactionTemplate.execute(status ->
                jdbcTemplate.update(DELETE_EXPIRED_SQL, deadline, batchSize));
        return deleted == null ? 0 : deleted;
    }
}
