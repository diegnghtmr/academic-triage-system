package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.infrastructure.idempotency.IdempotencyProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(IdempotencyProperties.class)
class IdempotencyConfiguration {
}
