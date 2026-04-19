package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.idempotency;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class IdempotencyCleanupScheduler {

    private final IdempotencyCleanupService cleanupService;

    public IdempotencyCleanupScheduler(IdempotencyCleanupService cleanupService) {
        this.cleanupService = Objects.requireNonNull(cleanupService, "cleanupService no puede ser null");
    }

    @Scheduled(cron = "${app.idempotency.cleanup.schedule:0 */10 * * * *}")
    public void runScheduledCleanup() {
        cleanupService.runCleanupBatch();
    }
}
