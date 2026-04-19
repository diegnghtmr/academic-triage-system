package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support;

import co.edu.uniquindio.triage.application.exception.IdempotencyFingerprintMismatchException;
import co.edu.uniquindio.triage.application.exception.IdempotencyRequestInProgressException;
import co.edu.uniquindio.triage.application.exception.MissingIdempotencyKeyException;
import co.edu.uniquindio.triage.infrastructure.idempotency.CanonicalFingerprintService;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyExecutionResult;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyExecutor;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyRequest;
import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyResponseEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HttpIdempotencySupportTest {

    @Mock
    private IdempotencyExecutor executor;

    private HttpIdempotencySupport support;

    @BeforeEach
    void setUp() {
        var objectMapper = new ObjectMapper();
        var fingerprintService = new CanonicalFingerprintService(objectMapper);
        support = new HttpIdempotencySupport(executor, fingerprintService, objectMapper);
    }

    @Test
    void mustThrowMissingIdempotencyKeyExceptionWhenKeyIsNull() {
        assertThatThrownBy(() -> support.execute(
                null, "scope", "POST", "/path", "user1", MediaType.APPLICATION_JSON_VALUE,
                Map.of("field", "value"), () -> ResponseEntity.ok("body")))
                .isInstanceOf(MissingIdempotencyKeyException.class);
    }

    @Test
    void mustThrowMissingIdempotencyKeyExceptionWhenKeyIsBlank() {
        assertThatThrownBy(() -> support.execute(
                "   ", "scope", "POST", "/path", "user1", MediaType.APPLICATION_JSON_VALUE,
                Map.of("field", "value"), () -> ResponseEntity.ok("body")))
                .isInstanceOf(MissingIdempotencyKeyException.class);
    }

    @Test
    void mustReturnFreshStatusOnProcessed() {
        var envelope = new IdempotencyResponseEnvelope(201, MediaType.APPLICATION_JSON_VALUE, Map.of(), "{\"id\":1}");
        given(executor.execute(any(IdempotencyRequest.class), any())).willReturn(IdempotencyExecutionResult.processed(envelope));

        var response = support.execute(
                "key-001", "scope", "POST", "/path", "user1",
                MediaType.APPLICATION_JSON_VALUE, Map.of(), () -> ResponseEntity.status(HttpStatus.CREATED).body("ignored"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getFirst("Idempotency-Status")).isEqualTo("fresh");
    }

    @Test
    void mustReturnReplayedStatusOnReplay() {
        var envelope = new IdempotencyResponseEnvelope(201, MediaType.APPLICATION_JSON_VALUE, Map.of(), "{\"id\":42}");
        given(executor.execute(any(IdempotencyRequest.class), any())).willReturn(IdempotencyExecutionResult.replay(envelope));

        var response = support.execute(
                "key-001", "scope", "POST", "/path", "user1",
                MediaType.APPLICATION_JSON_VALUE, Map.of(), () -> ResponseEntity.status(HttpStatus.CREATED).body("not called"));

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getFirst("Idempotency-Status")).isEqualTo("replayed");
    }

    @Test
    void mustPreserveReplayedBody() {
        var envelope = new IdempotencyResponseEnvelope(200, MediaType.APPLICATION_JSON_VALUE, Map.of(), "{\"id\":7}");
        given(executor.execute(any(IdempotencyRequest.class), any())).willReturn(IdempotencyExecutionResult.replay(envelope));

        var response = (ResponseEntity<byte[]>) support.execute(
                "key-002", "scope", "GET", "/path", "user1",
                MediaType.APPLICATION_JSON_VALUE, null, () -> ResponseEntity.ok("ignored"));

        assertThat(new String(response.getBody())).isEqualTo("{\"id\":7}");
    }

    @Test
    void mustPreserveLocationHeaderOnReplay() {
        var headers = Map.of(HttpHeaders.LOCATION, java.util.List.of("/api/v1/requests/99"));
        var envelope = new IdempotencyResponseEnvelope(201, MediaType.APPLICATION_JSON_VALUE, headers, null);
        given(executor.execute(any(IdempotencyRequest.class), any())).willReturn(IdempotencyExecutionResult.replay(envelope));

        var response = support.execute(
                "key-003", "scope", "POST", "/requests", "user1",
                MediaType.APPLICATION_JSON_VALUE, Map.of(), () -> ResponseEntity.status(201).build());

        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo("/api/v1/requests/99");
        assertThat(response.getHeaders().getFirst("Idempotency-Status")).isEqualTo("replayed");
    }

    @Test
    void mustThrowFingerprintMismatchOnMismatch() {
        given(executor.execute(any(IdempotencyRequest.class), any())).willReturn(IdempotencyExecutionResult.mismatch());

        assertThatThrownBy(() -> support.execute(
                "key-004", "scope", "POST", "/path", "user1",
                MediaType.APPLICATION_JSON_VALUE, Map.of("x", "1"), () -> ResponseEntity.ok("body")))
                .isInstanceOf(IdempotencyFingerprintMismatchException.class);
    }

    @Test
    void mustThrowRequestInProgressOnOutstanding() {
        given(executor.execute(any(IdempotencyRequest.class), any())).willReturn(IdempotencyExecutionResult.outstanding());

        assertThatThrownBy(() -> support.execute(
                "key-005", "scope", "POST", "/path", "user1",
                MediaType.APPLICATION_JSON_VALUE, Map.of(), () -> ResponseEntity.ok("body")))
                .isInstanceOf(IdempotencyRequestInProgressException.class);
    }

    @Test
    void processorIsInvokedOnProcessed() {
        var envelope = new IdempotencyResponseEnvelope(200, MediaType.APPLICATION_JSON_VALUE, Map.of(), "\"ok\"");
        given(executor.execute(any(IdempotencyRequest.class), any(Supplier.class))).willAnswer(inv -> {
            Supplier<IdempotencyResponseEnvelope> supplier = inv.getArgument(1);
            supplier.get();
            return IdempotencyExecutionResult.processed(envelope);
        });

        boolean[] called = {false};
        support.execute("key-006", "scope", "POST", "/path", "user1",
                MediaType.APPLICATION_JSON_VALUE, null,
                () -> { called[0] = true; return ResponseEntity.ok("ok"); });

        assertThat(called[0]).isTrue();
    }
}
