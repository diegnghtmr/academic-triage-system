package co.edu.uniquindio.triage.infrastructure.idempotency;

import java.time.Duration;

public final class IdempotencyTtlPolicy {

    private IdempotencyTtlPolicy() {}

    /**
     * Returns the TTL for a given operation scope. AI scopes get a shorter TTL
     * because classification suggestions are ephemeral and inexpensive to regenerate.
     * All other business-mutation scopes use the longer default to cover retry windows.
     */
    public static Duration ttlFor(String scope, IdempotencyProperties properties) {
        if (scope != null && scope.startsWith("ai:")) {
            return Duration.ofDays(properties.getTtl().getAiDays());
        }
        return Duration.ofDays(properties.getTtl().getDefaultDays());
    }
}
