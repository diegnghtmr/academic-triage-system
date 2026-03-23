package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter.UserPersistenceAdapter;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = UserPersistenceAdapterTest.TestApplication.class)
@Import(co.edu.uniquindio.triage.infrastructure.config.PersistenceConfiguration.class)
class UserPersistenceAdapterTest {

    private static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11");

    static {
        MARIADB.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
    }

    @Autowired
    private UserJpaRepository userJpaRepository;

    private UserPersistenceAdapter userPersistenceAdapter;

    @BeforeEach
    void setUp() {
        userPersistenceAdapter = new UserPersistenceAdapter(userJpaRepository, new UserPersistenceMapper());
    }

    @Test
    void saveAndLoadMustPreserveCanonicalUserMapping() {
        var saved = userPersistenceAdapter.save(newUser(
                "jperez",
                "jperez@uniquindio.edu.co",
                "1094123456",
                Role.STAFF
        ));

        var loadedByUsername = userPersistenceAdapter.loadByUsername(new Username("jperez"));

        assertThat(saved.getId()).isNotNull();
        assertThat(loadedByUsername).isPresent();
        assertThat(loadedByUsername.orElseThrow().getId()).isEqualTo(saved.getId());
        assertThat(loadedByUsername.orElseThrow().getFullName()).isEqualTo("Juan Pérez");
        assertThat(loadedByUsername.orElseThrow().getPasswordHash().value()).isEqualTo("hashed-password");
        assertThat(loadedByUsername.orElseThrow().getEmail().value()).isEqualTo("jperez@uniquindio.edu.co");
        assertThat(loadedByUsername.orElseThrow().getIdentification().value()).isEqualTo("1094123456");
        assertThat(loadedByUsername.orElseThrow().getRole()).isEqualTo(Role.STAFF);
        assertThat(loadedByUsername.orElseThrow().isActive()).isTrue();
        assertThat(userPersistenceAdapter.loadByEmail(new Email("jperez@uniquindio.edu.co"))).isPresent();
        assertThat(userPersistenceAdapter.existsByUsername(new Username("jperez"))).isTrue();
        assertThat(userPersistenceAdapter.existsByEmail(new Email("jperez@uniquindio.edu.co"))).isTrue();
        assertThat(userPersistenceAdapter.existsByIdentification(new Identification("1094123456"))).isTrue();
    }

    @Test
    void repositoryMustRejectDuplicateUsername() {
        userJpaRepository.saveAndFlush(entity("jperez", "jperez@uniquindio.edu.co", "1094123456"));

        assertThatThrownBy(() -> userJpaRepository.saveAndFlush(
                entity("jperez", "otro@uniquindio.edu.co", "2094123456")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void repositoryMustRejectDuplicateEmail() {
        userJpaRepository.saveAndFlush(entity("jperez", "jperez@uniquindio.edu.co", "1094123456"));

        assertThatThrownBy(() -> userJpaRepository.saveAndFlush(
                entity("mlopez", "jperez@uniquindio.edu.co", "2094123456")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void repositoryMustRejectDuplicateIdentification() {
        userJpaRepository.saveAndFlush(entity("jperez", "jperez@uniquindio.edu.co", "1094123456"));

        assertThatThrownBy(() -> userJpaRepository.saveAndFlush(
                entity("mlopez", "mlopez@uniquindio.edu.co", "1094123456")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private User newUser(String username, String email, String identification, Role role) {
        return User.registerNew(
                new Username(username),
                "Juan Pérez",
                new PasswordHash("hashed-password"),
                new Identification(identification),
                new Email(email),
                role
        );
    }

    private UserJpaEntity entity(String username, String email, String identification) {
        return new UserJpaEntity(
                null,
                username,
                email,
                identification,
                "Usuario Base",
                Role.STUDENT.name(),
                true,
                "hashed-password"
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
