package co.edu.uniquindio.triage;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = Application.class,
        properties = {
                "app.jwt.secret=12345678901234567890123456789012",
                "app.jwt.expiration-ms=86400000"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationRequestCreationSmokeTest {

    private static final String SECRET = "12345678901234567890123456789012";
    private static final String STUDENT_USERNAME = "request-smoke-student";
    private static final String DESCRIPTION = "Necesito validar que la creación de solicitudes funcione end to end en smoke testing";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    private long studentId;
    private long requestTypeId;
    private long originChannelId;

    @BeforeEach
    void setUp() throws Exception {
        try (var connection = dataSource.getConnection()) {
            cleanup(connection);
            studentId = insertUser(connection);
            requestTypeId = insertRequestType(connection);
            originChannelId = insertOriginChannel(connection);
        }
    }

    @Test
    void createRequestMustPersistAndReturn201ForAuthenticatedStudent() throws Exception {
        mockMvc.perform(post("/api/v1/requests")
                        .header("Authorization", "Bearer " + studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestTypeId": %d,
                                  "originChannelId": %d,
                                  "description": "%s",
                                  "deadline": "2026-05-15"
                                }
                                """.formatted(requestTypeId, originChannelId, DESCRIPTION)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/requests/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.requester.username").value(STUDENT_USERNAME))
                .andExpect(jsonPath("$.requestType.id").value(requestTypeId));

        try (var connection = dataSource.getConnection()) {
            var createdRequestId = findPersistedRequestId(connection);
            assertPersistedRequest(connection, createdRequestId);
            assertInitialHistory(connection, createdRequestId);
        }
    }

    private void cleanup(java.sql.Connection connection) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute("DELETE FROM request_history WHERE request_id IN (SELECT id FROM academic_requests WHERE description LIKE 'Necesito validar que la creación de solicitudes funcione end to end en smoke testing%')");
            statement.execute("DELETE FROM academic_requests WHERE description LIKE 'Necesito validar que la creación de solicitudes funcione end to end en smoke testing%'");
            statement.execute("DELETE FROM users WHERE username = '" + STUDENT_USERNAME + "'");
            statement.execute("DELETE FROM request_types WHERE name = 'Smoke Request Type'");
            statement.execute("DELETE FROM origin_channels WHERE name = 'Smoke Origin Channel'");
        }
    }

    private long findPersistedRequestId(java.sql.Connection connection) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT id
                FROM academic_requests
                WHERE applicant_id = ?
                  AND description = ?
                """)) {
            statement.setLong(1, studentId);
            statement.setString(2, DESCRIPTION);
            try (var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                var requestId = resultSet.getLong("id");
                assertThat(resultSet.next()).isFalse();
                return requestId;
            }
        }
    }

    private long insertUser(java.sql.Connection connection) throws Exception {
        try (var statement = connection.prepareStatement("""
                INSERT INTO users (username, email, identification, first_name, last_name, role, active, password_hash)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, STUDENT_USERNAME);
            statement.setString(2, STUDENT_USERNAME + "@uniquindio.edu.co");
            statement.setString(3, "200000000");
            statement.setString(4, "Request");
            statement.setString(5, "Smoke");
            statement.setString(6, "STUDENT");
            statement.setBoolean(7, true);
            statement.setString(8, "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
            statement.executeUpdate();
            try (var generatedKeys = statement.getGeneratedKeys()) {
                assertThat(generatedKeys.next()).isTrue();
                return generatedKeys.getLong(1);
            }
        }
    }

    private long insertRequestType(java.sql.Connection connection) throws Exception {
        try (var statement = connection.prepareStatement("""
                INSERT INTO request_types (name, description, active)
                VALUES (?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "Smoke Request Type");
            statement.setString(2, "Tipo de solicitud exclusivo para smoke testing");
            statement.setBoolean(3, true);
            statement.executeUpdate();
            try (var generatedKeys = statement.getGeneratedKeys()) {
                assertThat(generatedKeys.next()).isTrue();
                return generatedKeys.getLong(1);
            }
        }
    }

    private long insertOriginChannel(java.sql.Connection connection) throws Exception {
        try (var statement = connection.prepareStatement("""
                INSERT INTO origin_channels (name, active)
                VALUES (?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "Smoke Origin Channel");
            statement.setBoolean(2, true);
            statement.executeUpdate();
            try (var generatedKeys = statement.getGeneratedKeys()) {
                assertThat(generatedKeys.next()).isTrue();
                return generatedKeys.getLong(1);
            }
        }
    }

    private void assertPersistedRequest(java.sql.Connection connection, long requestId) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT applicant_id, request_type_id, origin_channel_id, status, description, deadline
                FROM academic_requests
                WHERE id = ?
                """)) {
            statement.setLong(1, requestId);
            try (var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getLong("applicant_id")).isEqualTo(studentId);
                assertThat(resultSet.getLong("request_type_id")).isEqualTo(requestTypeId);
                assertThat(resultSet.getLong("origin_channel_id")).isEqualTo(originChannelId);
                assertThat(resultSet.getString("status")).isEqualTo("REGISTERED");
                assertThat(resultSet.getString("description")).isEqualTo(DESCRIPTION);
                assertThat(resultSet.getDate("deadline").toLocalDate()).hasToString("2026-05-15");
                assertThat(resultSet.next()).isFalse();
            }
        }
    }

    private void assertInitialHistory(java.sql.Connection connection, long requestId) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT action, observations, performed_by_id
                FROM request_history
                WHERE request_id = ?
                ORDER BY id ASC
                """)) {
            statement.setLong(1, requestId);
            try (var resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("action")).isEqualTo("REGISTERED");
                assertThat(resultSet.getString("observations")).isEqualTo("Request registered");
                assertThat(resultSet.getLong("performed_by_id")).isEqualTo(studentId);
                assertThat(resultSet.next()).isFalse();
            }
        }
    }

    private String studentToken() {
        var secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(STUDENT_USERNAME)
                .claim("uid", studentId)
                .claim("role", "STUDENT")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(secretKey)
                .compact();
    }
}
