package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support;

import co.edu.uniquindio.triage.application.exception.IdempotencyFingerprintMismatchException;
import co.edu.uniquindio.triage.application.exception.IdempotencyRequestInProgressException;
import co.edu.uniquindio.triage.application.exception.MissingIdempotencyKeyException;
import co.edu.uniquindio.triage.infrastructure.idempotency.CanonicalFingerprintService;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyExecutor;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyFingerprintSource;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyRequest;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyResponseEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Component
public class HttpIdempotencySupport {

    private static final String IDEMPOTENCY_STATUS_HEADER = "Idempotency-Status";
    private static final String STATUS_FRESH = "fresh";
    private static final String STATUS_REPLAYED = "replayed";

    private final IdempotencyExecutor executor;
    private final CanonicalFingerprintService fingerprintService;
    private final ObjectMapper objectMapper;

    public HttpIdempotencySupport(IdempotencyExecutor executor,
                                   CanonicalFingerprintService fingerprintService,
                                   ObjectMapper objectMapper) {
        this.executor = Objects.requireNonNull(executor, "executor no puede ser null");
        this.fingerprintService = Objects.requireNonNull(fingerprintService, "fingerprintService no puede ser null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper no puede ser null");
    }

    public ResponseEntity<?> execute(String idempotencyKey,
                                     String operationScope,
                                     String httpMethod,
                                     String path,
                                     String principalScope,
                                     String contentType,
                                     Object body,
                                     Supplier<ResponseEntity<?>> processor) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }

        var fingerprintSource = new IdempotencyFingerprintSource(
                operationScope,
                httpMethod,
                path,
                Map.of(),
                contentType == null ? "" : contentType,
                body
        );
        var fingerprint = fingerprintService.fingerprint(fingerprintSource);
        var request = new IdempotencyRequest(
                operationScope,
                principalScope == null ? "" : principalScope,
                idempotencyKey,
                fingerprint
        );

        var result = executor.execute(request, () -> {
            var response = processor.get();
            return IdempotencyResponseEnvelope.fromResponseEntity(response, objectMapper);
        });

        return switch (result.status()) {
            case PROCESSED -> withStatus(result.response().orElseThrow().toRawResponseEntity(), STATUS_FRESH);
            case REPLAY    -> withStatus(result.response().orElseThrow().toRawResponseEntity(), STATUS_REPLAYED);
            case MISMATCH  -> throw new IdempotencyFingerprintMismatchException();
            case OUTSTANDING -> throw new IdempotencyRequestInProgressException();
        };
    }

    private ResponseEntity<byte[]> withStatus(ResponseEntity<byte[]> response, String idempotencyStatus) {
        return ResponseEntity
                .status(response.getStatusCode())
                .headers(h -> {
                    h.addAll(response.getHeaders());
                    h.set(IDEMPOTENCY_STATUS_HEADER, idempotencyStatus);
                })
                .body(response.getBody());
    }
}
