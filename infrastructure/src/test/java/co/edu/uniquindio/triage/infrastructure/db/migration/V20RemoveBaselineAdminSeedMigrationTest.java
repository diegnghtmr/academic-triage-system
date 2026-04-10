package co.edu.uniquindio.triage.infrastructure.db.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * V20 elimina el admin del baseline; V21 (solo {@code classpath:db/migration-dev}) lo restaura para local.
 */
class V20RemoveBaselineAdminSeedMigrationTest {

    private static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11");

    static {
        MARIADB.start();
    }

    @Test
    void v20LeavesNoAdminWhenOnlySharedMigrationsRun() throws Exception {
        String url = MARIADB.getJdbcUrl();
        String user = MARIADB.getUsername();
        String password = MARIADB.getPassword();

        Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load()
                .clean();

        Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection c = DriverManager.getConnection(url, user, password);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'admin'")) {
            rs.next();
            assertThat(rs.getInt(1)).isZero();
        }
    }

    @Test
    void devMigrationsRestoreLocalAdminAfterV20() throws Exception {
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
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'admin'")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }
}
