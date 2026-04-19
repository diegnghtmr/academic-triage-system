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

class V24AddIdempotencyAndOptimisticLockingMigrationTest {

    private static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11");

    static {
        MARIADB.start();
    }

    @Test
    void migrationMustCreateIdempotencyTableAndVersionColumns() throws Exception {
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

            assertThat(loadColumns(statement, "idempotency_keys"))
                    .contains("id", "scope", "idempotency_key", "fingerprint", "status", "response_status_code",
                            "response_headers", "response_body", "response_content_type", "created_at", "updated_at", "completed_at");
            assertThat(loadColumns(statement, "academic_requests")).contains("version");
            assertThat(loadColumns(statement, "users")).contains("version");
            assertThat(loadColumns(statement, "request_types")).contains("version");
            assertThat(loadColumns(statement, "origin_channels")).contains("version");
            assertThat(loadColumns(statement, "business_rules")).contains("version");
        }
    }

    private Set<String> loadColumns(Statement statement, String tableName) throws Exception {
        var columns = new HashSet<String>();
        try (ResultSet resultSet = statement.executeQuery(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '" + tableName + "'")) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("column_name"));
            }
        }
        return columns;
    }
}
