package co.edu.uniquindio.triage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that /actuator/prometheus is properly exposed and returns Prometheus-format metrics.
 * Idempotency metrics appear after first use (lazy registration); this test validates the
 * endpoint availability and base JVM/Spring metrics that are always exported.
 *
 * Security is excluded entirely so the endpoint is reachable without credentials.
 * The goal is to verify Micrometer/Prometheus wiring, not security rules.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = ApplicationPrometheusEndpointTest.PrometheusTestApplication.class,
        properties = {
            "app.jwt.secret=12345678901234567890123456789012",
            "app.jwt.expiration-ms=86400000"
        })
@AutoConfigureObservability
class ApplicationPrometheusEndpointTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void prometheusEndpointMustBeAvailableAndReturnMetrics() {
        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/prometheus",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString())
                .contains("text/plain");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("jvm_memory_used_bytes");
        assertThat(response.getBody()).contains("process_uptime_seconds");
    }

    @Test
    void prometheusEndpointMustBeListedInActuatorLinks() {
        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("prometheus");
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            UserDetailsServiceAutoConfiguration.class,
            ManagementWebSecurityAutoConfiguration.class
    })
    static class PrometheusTestApplication {
    }
}
