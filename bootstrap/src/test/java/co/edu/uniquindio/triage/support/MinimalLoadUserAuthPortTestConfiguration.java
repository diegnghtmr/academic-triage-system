package co.edu.uniquindio.triage.support;

import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

/**
 * Provides {@link LoadUserAuthPort} for slim bootstrap tests that import
 * {@link co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration} without the persistence stack.
 * <p>
 * Uses {@link TestConfiguration} so it is not picked up by {@code @SpringBootApplication} component scanning
 * during full-context tests (avoids clashing with {@code UserPersistenceAdapter}).
 * <p>
 * Bearer tokens are resolved against this port by {@code JwtAuthenticationFilter}; returning an empty
 * optional leaves the request unauthenticated after the filter runs, which is fine for {@code permitAll}
 * endpoints (health, public docs). Protected routes in these slices are not exercised.
 */
@TestConfiguration
public class MinimalLoadUserAuthPortTestConfiguration {

    @Bean
    LoadUserAuthPort loadUserAuthPort() {
        return new LoadUserAuthPort() {
            @Override
            public Optional<User> loadByUsername(Username username) {
                return Optional.empty();
            }

            @Override
            public Optional<User> loadByEmail(Email email) {
                return Optional.empty();
            }

            @Override
            public Optional<User> loadById(UserId id) {
                return Optional.empty();
            }

            @Override
            public boolean existsByUsername(Username username) {
                return false;
            }

            @Override
            public boolean existsByEmail(Email email) {
                return false;
            }

            @Override
            public boolean existsByIdentification(Identification identification) {
                return false;
            }
        };
    }
}
