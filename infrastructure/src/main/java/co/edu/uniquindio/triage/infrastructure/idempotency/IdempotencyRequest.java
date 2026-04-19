package co.edu.uniquindio.triage.infrastructure.idempotency;

public record IdempotencyRequest(String scope, String principalScope, String key, String fingerprint) {

    public IdempotencyRequest {
        scope = requireText(scope, "El scope es obligatorio");
        principalScope = principalScope == null ? "" : principalScope.strip();
        key = requireText(key, "La idempotency key es obligatoria");
        fingerprint = requireText(fingerprint, "El fingerprint es obligatorio");
    }

    public static IdempotencyRequest of(String scope, String key, String fingerprint) {
        return new IdempotencyRequest(scope, "", key, fingerprint);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
