package co.edu.uniquindio.triage.infrastructure.db.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MariaDBContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class V25AddIdempotencyExtendedFieldsMigrationTest {

    private static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11");

    static {
        MARIADB.start();
    }

    @Test
    void migrationMustAddPrincipalScopeAndMetadataColumnsToIdempotencyKeys() throws Exception {
        var url = MARIADB.getJdbcUrl();
        var user = MARIADB.getUsername();
        var password = MARIADB.getPassword();

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

        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            var columns = loadColumns(statement, "idempotency_keys");
            assertThat(columns).contains(
                    "principal_scope", "resource_type", "resource_id",
                    "correlation_id", "expires_at", "last_seen_at"
            );

            assertThat(loadIndexNames(statement, "idempotency_keys"))
                    .contains("uk_idempotency_scope_principal_key", "idx_idempotency_expires_at");
        }
    }

    private Set<String> loadColumns(Statement statement, String tableName) throws Exception {
        var columns = new HashSet<String>();
        try (ResultSet rs = statement.executeQuery(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = '" + tableName + "'")) {
            while (rs.next()) {
                columns.add(rs.getString("column_name"));
            }
        }
        return columns;
    }

    private Set<String> loadIndexNames(Statement statement, String tableName) throws Exception {
        var indexes = new HashSet<String>();
        try (ResultSet rs = statement.executeQuery(
                "SELECT index_name FROM information_schema.statistics " +
                "WHERE table_schema = DATABASE() AND table_name = '" + tableName + "'")) {
            while (rs.next()) {
                indexes.add(rs.getString("index_name"));
            }
        }
        return indexes;
    }
}
