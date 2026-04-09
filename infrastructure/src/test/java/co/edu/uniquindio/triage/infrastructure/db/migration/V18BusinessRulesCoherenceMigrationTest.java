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
 * Evidencia conductual de {@code V18__business_rules_coherence.sql} sobre datos legacy reales:
 * canonización segura, desactivación de filas no determinísticas y FK limpia en DEADLINE/IMPACT_LEVEL.
 */
class V18BusinessRulesCoherenceMigrationTest {

    private static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11");

    static {
        MARIADB.start();
    }

    @Test
    void v18CanonizesCompatibleLegacyRulesAndDeactivatesUnsafeRows() throws Exception {
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
                .target("17")
                .load()
                .migrate();

        long homologacionId;
        try (Connection c = DriverManager.getConnection(url, user, password);
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id FROM request_types WHERE name = 'Homologación'")) {
            assertThat(rs.next()).isTrue();
            homologacionId = rs.getLong(1);
        }

        try (Connection c = DriverManager.getConnection(url, user, password);
             Statement s = c.createStatement()) {
            s.executeUpdate(
                    """
                    INSERT INTO business_rules \
                    (name, description, condition_type, condition_value, resulting_priority, active, request_type_id) VALUES \
                    ('v18_legacy_by_name', 't', 'REQUEST_TYPE', 'Homologación', 'HIGH', TRUE, NULL), \
                    ('v18_legacy_numeric_id', 't', 'REQUEST_TYPE', '%s', 'MEDIUM', TRUE, NULL), \
                    ('v18_legacy_garbage_type', 't', 'REQUEST_TYPE', 'zzz tipo inexistente para v18', 'LOW', TRUE, NULL), \
                    ('v18_legacy_rtd_bad', 't', 'REQUEST_TYPE_AND_DEADLINE', 'no_days', 'HIGH', TRUE, NULL), \
                    ('v18_legacy_deadline_bad', 't', 'DEADLINE', 'not-numeric', 'HIGH', TRUE, NULL), \
                    ('v18_legacy_deadline_bad_fk', 't', 'DEADLINE', '7', 'MEDIUM', TRUE, %d), \
                    ('v18_legacy_impact_bad', 't', 'IMPACT_LEVEL', 'NOT_A_LEVEL', 'MEDIUM', TRUE, NULL), \
                    ('v18_legacy_impact_case', 't', 'IMPACT_LEVEL', 'low', 'LOW', TRUE, NULL) \
                    """
                            .formatted(Long.toString(homologacionId), homologacionId));
        }

        Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection c = DriverManager.getConnection(url, user, password);
             Statement s = c.createStatement()) {

            assertLegacyRuleCanonizedByName(s, homologacionId);
            assertLegacyNumericIdLinked(s, homologacionId);
            assertRuleInactive(s, "v18_legacy_garbage_type");
            assertRuleInactive(s, "v18_legacy_rtd_bad");
            assertRuleInactive(s, "v18_legacy_deadline_bad");
            assertRuleInactive(s, "v18_legacy_impact_bad");

            assertLegacyDeadlineFkCleared(s, "v18_legacy_deadline_bad_fk");

            try (ResultSet rs = s.executeQuery(
                    "SELECT condition_value, active FROM business_rules WHERE name = 'v18_legacy_impact_case'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("condition_value")).isEqualTo("LOW");
                assertThat(rs.getBoolean("active")).isTrue();
            }

            try (ResultSet rs = s.executeQuery(
                    """
                    SELECT condition_value, request_type_id, active FROM business_rules \
                    WHERE name = 'Tipo reintegro'\
                    """)) {
                assertThat(rs.next()).isTrue();
                long rtId = rs.getLong("request_type_id");
                assertThat(rs.getBoolean("active")).isTrue();
                assertThat(rs.getString("condition_value")).isEqualTo(String.valueOf(rtId));
            }
        }
    }

    private static void assertLegacyRuleCanonizedByName(Statement s, long expectedTypeId) throws Exception {
        try (ResultSet rs = s.executeQuery(
                "SELECT condition_value, request_type_id, active FROM business_rules WHERE name = 'v18_legacy_by_name'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getBoolean("active")).isTrue();
            assertThat(rs.getLong("request_type_id")).isEqualTo(expectedTypeId);
            assertThat(rs.getString("condition_value")).isEqualTo(String.valueOf(expectedTypeId));
        }
    }

    private static void assertLegacyNumericIdLinked(Statement s, long expectedTypeId) throws Exception {
        try (ResultSet rs = s.executeQuery(
                "SELECT condition_value, request_type_id, active FROM business_rules WHERE name = 'v18_legacy_numeric_id'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getBoolean("active")).isTrue();
            assertThat(rs.getLong("request_type_id")).isEqualTo(expectedTypeId);
            assertThat(rs.getString("condition_value")).isEqualTo(String.valueOf(expectedTypeId));
        }
    }

    private static void assertRuleInactive(Statement s, String name) throws Exception {
        try (ResultSet rs = s.executeQuery(
                "SELECT active FROM business_rules WHERE name = '" + name + "'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getBoolean("active")).isFalse();
        }
    }

    private static void assertLegacyDeadlineFkCleared(Statement s, String name) throws Exception {
        try (ResultSet rs = s.executeQuery(
                "SELECT request_type_id, condition_value, active FROM business_rules WHERE name = '"
                        + name
                        + "'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getObject("request_type_id")).isNull();
            assertThat(rs.getString("condition_value")).isEqualTo("7");
            assertThat(rs.getBoolean("active")).isTrue();
        }
    }
}
