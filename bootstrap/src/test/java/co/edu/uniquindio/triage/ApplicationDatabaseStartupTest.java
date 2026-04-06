package co.edu.uniquindio.triage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = Application.class,
        properties = {
                "app.jwt.secret=12345678901234567890123456789012",
                "app.jwt.expiration-ms=86400000"
        }
)
@ActiveProfiles("test")
class ApplicationDatabaseStartupTest {

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
    private DataSource dataSource;

    @Test
    void applicationMustBootOnCleanDatabaseWithFlywayAndJpaValidation() throws Exception {
        try (var connection = dataSource.getConnection();
             var migrationsStatement = connection.prepareStatement(
                     "select count(*) from flyway_schema_history where success = true");
             var adminUsersStatement = connection.prepareStatement(
                     "select count(*) from users where username = 'admin'")) {

            var migrationsResult = migrationsStatement.executeQuery();
            migrationsResult.next();
            var appliedMigrations = migrationsResult.getInt(1);

            var adminUsersResult = adminUsersStatement.executeQuery();
            adminUsersResult.next();
            var adminUsers = adminUsersResult.getInt(1);

            assertThat(appliedMigrations).isGreaterThanOrEqualTo(10);
            assertThat(adminUsers).isEqualTo(1);
        }
    }
}
