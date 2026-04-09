package co.edu.uniquindio.triage.infrastructure.testsupport;

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
 * No-op {@link LoadUserAuthPort} for WebMvc/security slices that import {@code SecurityConfiguration}
 * without JPA. JWT filter calls this port on Bearer requests; returning empty keeps the request
 * unauthenticated (sufficient when tests use {@code SecurityMockMvcRequestPostProcessors}).
 */
@TestConfiguration
public class NoopLoadUserAuthPortTestConfiguration {

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
