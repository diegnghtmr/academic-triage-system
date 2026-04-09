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
 * Evidencia de {@code V19__remove_impact_level_business_rules.sql}: borrado físico solo de IMPACT_LEVEL;
 * reglas válidas permanecen.
 */
class V19RemoveImpactLevelBusinessRulesMigrationTest {

    private static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11");

    static {
        MARIADB.start();
    }

    @Test
    void v19DeletesImpactLevelRowsAndKeepsOtherRules() throws Exception {
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
                .target("18")
                .load()
                .migrate();

        try (Connection c = DriverManager.getConnection(url, user, password);
             Statement s = c.createStatement()) {
            s.executeUpdate(
                    """
                    INSERT INTO business_rules \
                    (name, description, condition_type, condition_value, resulting_priority, active, request_type_id) VALUES \
                    ('v19_impact_a', 't', 'IMPACT_LEVEL', 'HIGH', 'HIGH', TRUE, NULL), \
                    ('v19_impact_b', 't', 'IMPACT_LEVEL', 'LOW', 'LOW', TRUE, NULL), \
                    ('v19_deadline_ok', 't', 'DEADLINE', '3', 'MEDIUM', TRUE, NULL) \
                    """);
        }

        try (Connection c = DriverManager.getConnection(url, user, password);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) AS n FROM business_rules WHERE condition_type = 'IMPACT_LEVEL'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("n")).isEqualTo(2);
        }

        Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection c = DriverManager.getConnection(url, user, password);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) AS n FROM business_rules WHERE condition_type = 'IMPACT_LEVEL'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt("n")).isZero();
        }

        try (Connection c = DriverManager.getConnection(url, user, password);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT condition_value, active FROM business_rules WHERE name = 'v19_deadline_ok'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("condition_value")).isEqualTo("3");
            assertThat(rs.getBoolean("active")).isTrue();
        }
    }
}
