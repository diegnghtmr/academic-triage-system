package co.edu.uniquindio.triage.infrastructure.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalFingerprintServiceTest {

    private final CanonicalFingerprintService fingerprintService =
            new CanonicalFingerprintService(new ObjectMapper().findAndRegisterModules());

    @Test
    void fingerprintMustStayStableAcrossEquivalentJsonAndQueryOrdering() {
        var first = new IdempotencyFingerprintSource(
                "requests:create",
                "post",
                "/api/v1/requests",
                Map.of("b", List.of("2"), "a", List.of("3", "1")),
                "application/json",
                """
                        {"description":"Solicitud","meta":{"x":1,"y":2}}
                        """
        );
        var second = new IdempotencyFingerprintSource(
                "requests:create",
                "POST",
                "/api/v1/requests",
                Map.of("a", List.of("1", "3"), "b", List.of("2")),
                "application/json",
                """
                        {"meta":{"y":2,"x":1},"description":"Solicitud"}
                        """
        );

        assertThat(fingerprintService.fingerprint(first)).isEqualTo(fingerprintService.fingerprint(second));
    }

    @Test
    void fingerprintMustChangeWhenPayloadChanges() {
        var first = new IdempotencyFingerprintSource(
                "requests:create",
                "POST",
                "/api/v1/requests",
                Map.of(),
                "application/json",
                Map.of("description", "Solicitud A")
        );
        var second = new IdempotencyFingerprintSource(
                "requests:create",
                "POST",
                "/api/v1/requests",
                Map.of(),
                "application/json",
                Map.of("description", "Solicitud B")
        );

        assertThat(fingerprintService.fingerprint(first)).isNotEqualTo(fingerprintService.fingerprint(second));
    }
}
