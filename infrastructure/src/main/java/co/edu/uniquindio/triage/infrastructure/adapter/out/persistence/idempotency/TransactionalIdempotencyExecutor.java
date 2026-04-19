package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.idempotency;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.IdempotencyKeyJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.IdempotencyKeyJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.support.MariaDbUniqueViolation;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyExecutionResult;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyExecutor;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyProperties;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyRequest;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyResponseEnvelope;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyTtlPolicy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@Component
public class TransactionalIdempotencyExecutor implements IdempotencyExecutor {

    private static final TypeReference<Map<String, List<String>>> HEADERS_TYPE = new TypeReference<>() {};
    private static final String INSERT_CLAIM_SQL = """
            insert into idempotency_keys (
                scope,
                principal_scope,
                idempotency_key,
                fingerprint,
                status,
                expires_at,
                last_seen_at,
                created_at,
                updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final IdempotencyKeyJpaRepository idempotencyKeyJpaRepository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate claimTransactionTemplate;
    private final TransactionTemplate processingTransactionTemplate;
    private final TransactionTemplate cleanupTransactionTemplate;

    private final Counter requestsTotal;
    private final Counter replaysTotal;
    private final Counter mismatchesTotal;
    private final Counter outstandingTotal;
    private final Timer claimTimer;
    private final Timer replayTimer;
    private final IdempotencyProperties idempotencyProperties;

    public TransactionalIdempotencyExecutor(IdempotencyKeyJpaRepository idempotencyKeyJpaRepository,
                                            ObjectMapper objectMapper,
                                            JdbcTemplate jdbcTemplate,
                                            PlatformTransactionManager transactionManager,
                                            MeterRegistry meterRegistry,
                                            IdempotencyProperties idempotencyProperties) {
        this.idempotencyKeyJpaRepository = Objects.requireNonNull(idempotencyKeyJpaRepository, "El idempotencyKeyJpaRepository no puede ser null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "El objectMapper no puede ser null");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "El jdbcTemplate no puede ser null");
        Objects.requireNonNull(transactionManager, "El transactionManager no puede ser null");
        Objects.requireNonNull(meterRegistry, "El meterRegistry no puede ser null");
        this.idempotencyProperties = Objects.requireNonNull(idempotencyProperties, "El idempotencyProperties no puede ser null");

        this.claimTransactionTemplate = new TransactionTemplate(transactionManager);
        this.claimTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        this.processingTransactionTemplate = new TransactionTemplate(transactionManager);
        this.processingTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        this.cleanupTransactionTemplate = new TransactionTemplate(transactionManager);
        this.cleanupTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        this.requestsTotal = Counter.builder("idempotency.requests.total")
                .description("Total idempotency-guarded requests processed")
                .register(meterRegistry);
        this.replaysTotal = Counter.builder("idempotency.replays.total")
                .description("Requests served from idempotency cache (replayed)")
                .register(meterRegistry);
        this.mismatchesTotal = Counter.builder("idempotency.mismatches.total")
                .description("Requests with fingerprint mismatch for an existing idempotency key")
                .register(meterRegistry);
        this.outstandingTotal = Counter.builder("idempotency.outstanding.total")
                .description("Requests rejected because the key is still in-flight")
                .register(meterRegistry);
        this.claimTimer = Timer.builder("idempotency.claim.latency")
                .description("Time to claim or resolve an idempotency key")
                .register(meterRegistry);
        this.replayTimer = Timer.builder("idempotency.replay.latency")
                .description("Time to serve a replayed response from the idempotency cache")
                .register(meterRegistry);
    }

    public IdempotencyExecutionResult execute(IdempotencyRequest request,
                                              Supplier<IdempotencyResponseEnvelope> processor) {
        Objects.requireNonNull(request, "El request no puede ser null");
        Objects.requireNonNull(processor, "El processor no puede ser null");

        requestsTotal.increment();

        var claimDecision = claimTimer.record(() -> claim(request));

        if (claimDecision.replay().isPresent()) {
            replaysTotal.increment();
            return replayTimer.record(() -> IdempotencyExecutionResult.replay(claimDecision.replay().orElseThrow()));
        }
        if (claimDecision.status() == ClaimStatus.MISMATCH) {
            mismatchesTotal.increment();
            return IdempotencyExecutionResult.mismatch();
        }
        if (claimDecision.status() == ClaimStatus.OUTSTANDING) {
            outstandingTotal.increment();
            return IdempotencyExecutionResult.outstanding();
        }

        try {
            return processingTransactionTemplate.execute(status -> {
                var response = Objects.requireNonNull(processor.get(), "El processor debe devolver un envelope");
                complete(claimDecision.recordId().orElseThrow(), response);
                return IdempotencyExecutionResult.processed(response);
            });
        } catch (RuntimeException exception) {
            cleanupClaim(claimDecision.recordId().orElseThrow());
            throw exception;
        }
    }

    public ClaimDecision claim(IdempotencyRequest request) {
        Objects.requireNonNull(request, "El request no puede ser null");

        return claimTransactionTemplate.execute(status -> attemptClaim(request));
    }

    private ClaimDecision attemptClaim(IdempotencyRequest request) {
        var now = LocalDateTime.now();
        var expiresAt = now.plus(IdempotencyTtlPolicy.ttlFor(request.scope(), idempotencyProperties));

        try {
            jdbcTemplate.update(
                    INSERT_CLAIM_SQL,
                    request.scope(),
                    request.principalScope(),
                    request.key(),
                    request.fingerprint(),
                    IdempotencyStatus.PROCESSING.name(),
                    expiresAt,
                    now,
                    now,
                    now
            );
            var persisted = idempotencyKeyJpaRepository
                    .findByScopeAndPrincipalScopeAndIdempotencyKey(request.scope(), request.principalScope(), request.key())
                    .orElseThrow(() -> new IllegalStateException("No se encontró la clave de idempotencia luego de insertarla"));
            return ClaimDecision.claimed(persisted.getId());
        } catch (DataIntegrityViolationException exception) {
            if (!MariaDbUniqueViolation.isUniqueViolation(exception)) {
                throw exception;
            }
            var existing = idempotencyKeyJpaRepository
                    .findByScopeAndPrincipalScopeAndIdempotencyKeyForUpdate(request.scope(), request.principalScope(), request.key())
                    .orElseThrow(() -> new IllegalStateException("No se encontró la clave de idempotencia luego de detectar conflicto único"));
            if (!existing.getFingerprint().equals(request.fingerprint())) {
                return ClaimDecision.mismatch();
            }
            if (IdempotencyStatus.COMPLETED.name().equals(existing.getStatus())) {
                existing.setLastSeenAt(now);
                idempotencyKeyJpaRepository.save(existing);
                return ClaimDecision.replay(toEnvelope(existing));
            }
            return ClaimDecision.outstanding();
        }
    }

    private void complete(Long recordId, IdempotencyResponseEnvelope response) {
        var entity = idempotencyKeyJpaRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("No se encontró el registro de idempotencia reclamado"));
        var now = LocalDateTime.now();
        entity.setStatus(IdempotencyStatus.COMPLETED.name());
        entity.setResponseStatusCode(response.statusCode());
        entity.setResponseContentType(response.contentType());
        entity.setResponseBody(response.body());
        entity.setResponseHeaders(serializeHeaders(response.headers()));
        entity.setCompletedAt(now);
        entity.setUpdatedAt(now);
        idempotencyKeyJpaRepository.save(entity);
    }

    private void cleanupClaim(Long recordId) {
        cleanupTransactionTemplate.executeWithoutResult(status ->
                idempotencyKeyJpaRepository.findById(recordId)
                        .filter(entity -> IdempotencyStatus.PROCESSING.name().equals(entity.getStatus()))
                        .ifPresent(idempotencyKeyJpaRepository::delete));
    }

    private IdempotencyResponseEnvelope toEnvelope(IdempotencyKeyJpaEntity entity) {
        return new IdempotencyResponseEnvelope(
                entity.getResponseStatusCode(),
                entity.getResponseContentType(),
                deserializeHeaders(entity.getResponseHeaders()),
                entity.getResponseBody()
        );
    }

    private String serializeHeaders(Map<String, List<String>> headers) {
        try {
            return objectMapper.writeValueAsString(headers == null ? Map.of() : headers);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible serializar los headers de la respuesta idempotente", exception);
        }
    }

    private Map<String, List<String>> deserializeHeaders(String serializedHeaders) {
        if (serializedHeaders == null || serializedHeaders.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(serializedHeaders, HEADERS_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible deserializar los headers idempotentes almacenados", exception);
        }
    }

    public enum IdempotencyStatus {
        PROCESSING,
        COMPLETED
    }

    public enum ClaimStatus {
        CLAIMED,
        REPLAY,
        MISMATCH,
        OUTSTANDING
    }

    public record ClaimDecision(ClaimStatus status,
                                Optional<Long> recordId,
                                Optional<IdempotencyResponseEnvelope> replay) {
        public ClaimDecision {
            Objects.requireNonNull(status, "El status no puede ser null");
            recordId = recordId == null ? Optional.empty() : recordId;
            replay = replay == null ? Optional.empty() : replay;
        }

        static ClaimDecision claimed(Long recordId) {
            return new ClaimDecision(ClaimStatus.CLAIMED, Optional.of(recordId), Optional.empty());
        }

        static ClaimDecision replay(IdempotencyResponseEnvelope response) {
            return new ClaimDecision(ClaimStatus.REPLAY, Optional.empty(), Optional.of(response));
        }

        static ClaimDecision mismatch() {
            return new ClaimDecision(ClaimStatus.MISMATCH, Optional.empty(), Optional.empty());
        }

        static ClaimDecision outstanding() {
            return new ClaimDecision(ClaimStatus.OUTSTANDING, Optional.empty(), Optional.empty());
        }
    }
}
