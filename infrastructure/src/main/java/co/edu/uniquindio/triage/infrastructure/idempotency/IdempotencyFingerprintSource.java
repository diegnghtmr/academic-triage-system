package co.edu.uniquindio.triage.infrastructure.idempotency;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record IdempotencyFingerprintSource(
        String scope,
        String httpMethod,
        String path,
        Map<String, List<String>> queryParameters,
        String contentType,
        Object body
) {
    public IdempotencyFingerprintSource {
        Objects.requireNonNull(scope, "El scope no puede ser null");
        Objects.requireNonNull(httpMethod, "El httpMethod no puede ser null");
        Objects.requireNonNull(path, "El path no puede ser null");
        queryParameters = queryParameters == null ? Map.of() : Map.copyOf(queryParameters);
    }
}
