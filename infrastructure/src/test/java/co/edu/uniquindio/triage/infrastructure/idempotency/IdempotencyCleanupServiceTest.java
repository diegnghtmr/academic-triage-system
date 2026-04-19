package co.edu.uniquindio.triage.infrastructure.idempotency;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.idempotency.IdempotencyCleanupService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdempotencyCleanupServiceTest {

    @Test
    void runCleanupBatchMustIncrementFailureCounterWhenJdbcThrows() {
        var jdbcTemplate = mock(JdbcTemplate.class);
        var transactionManager = mock(PlatformTransactionManager.class);
        var txStatus = mock(TransactionStatus.class);

        when(transactionManager.getTransaction(any())).thenReturn(txStatus);
        when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenThrow(new RuntimeException("DB connection lost"));

        var meterRegistry = new SimpleMeterRegistry();
        var service = new IdempotencyCleanupService(jdbcTemplate, transactionManager,
                new IdempotencyProperties(), meterRegistry);

        assertThatCode(() -> service.runCleanupBatch()).doesNotThrowAnyException();

        assertThat(meterRegistry.counter("idempotency.cleanup.failures.total").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("idempotency.cleanup.runs.total").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("idempotency.cleanup.deleted.total").count()).isZero();
    }
}
