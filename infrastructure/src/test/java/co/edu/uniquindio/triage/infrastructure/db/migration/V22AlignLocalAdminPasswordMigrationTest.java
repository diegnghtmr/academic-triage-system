package co.edu.uniquindio.triage.infrastructure.db.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.MariaDBContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class V22AlignLocalAdminPasswordMigrationTest {

    private static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11");

    static {
        MARIADB.start();
    }

    @Test
    void devMigrationsMustLeaveLocalAdminActiveWithDocumentedPassword() throws Exception {
        String url = MARIADB.getJdbcUrl();
        String user = MARIADB.getUsername();
        String password = MARIADB.getPassword();

        Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration", "classpath:db/migration-dev")
                .cleanDisabled(false)
                .load()
                .clean();

        Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration", "classpath:db/migration-dev")
                .load()
                .migrate();

        try (Connection c = DriverManager.getConnection(url, user, password);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT active, password_hash FROM users WHERE username = 'admin'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getBoolean("active")).isTrue();
            assertThat(new BCryptPasswordEncoder().matches("admin123", rs.getString("password_hash"))).isTrue();
        }
    }
}
