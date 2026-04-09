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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = Application.class,
        properties = {
                "app.ai.provider=none",
                "app.jwt.secret=12345678901234567890123456789012",
                "app.jwt.expiration-ms=86400000"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationAiDisabledBehaviorTest {

    private static final String JWT_SECRET = "12345678901234567890123456789012";
    private static final String STAFF_USERNAME = "ai_disabled_staff";
    /** Same BCrypt digest as seeded admin in Flyway — only used to satisfy NOT NULL on password_hash. */
    private static final String PASSWORD_HASH_PLACEHOLDER =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    private String staffToken;
    private long summarizeRequestId;

    @BeforeEach
    void setup() throws Exception {
        try (var conn = dataSource.getConnection(); var st = conn.createStatement()) {
            st.execute(
                    "DELETE FROM request_history WHERE request_id IN (SELECT id FROM academic_requests WHERE description = 'AI disabled behavior test request')");
            st.execute("DELETE FROM academic_requests WHERE description = 'AI disabled behavior test request'");
            st.execute("DELETE FROM users WHERE username = '" + STAFF_USERNAME + "'");
        }

        long staffId;
        try (var conn = dataSource.getConnection()) {
            staffId = insertStaffUser(conn);
            summarizeRequestId = insertMinimalRequestForSummarize(conn, staffId);
        }

        var key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        staffToken = Jwts.builder()
                .subject(STAFF_USERNAME)
                .claim("uid", staffId)
                .claim("role", "STAFF")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key)
                .compact();
    }

    private long insertStaffUser(java.sql.Connection connection) throws Exception {
        try (var ps = connection.prepareStatement(
                """
                        INSERT INTO users (username, email, identification, first_name, last_name, role, active, password_hash)
                        VALUES (?, ?, ?, ?, ?, 'STAFF', TRUE, ?)
                        """,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, STAFF_USERNAME);
            ps.setString(2, STAFF_USERNAME + "@uniquindio.test");
            ps.setString(3, "ai-dis-staff-id");
            ps.setString(4, "Prueba");
            ps.setString(5, "StaffIA");
            ps.setString(6, PASSWORD_HASH_PLACEHOLDER);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /**
     * Needs a real row for {@link GenerateSummaryUseCase} before AI throws {@code AiServiceUnavailableException}.
     */
    private long insertMinimalRequestForSummarize(java.sql.Connection connection, long applicantId) throws Exception {
        try (var ps = connection.prepareStatement(
                """
                        INSERT INTO academic_requests (
                            description,
                            status,
                            registration_date,
                            applicant_id,
                            origin_channel_id,
                            request_type_id,
                            ai_suggested,
                            created_at,
                            updated_at
                        ) VALUES (?, 'REGISTERED', NOW(), ?, 1, 1, FALSE, NOW(), NOW())
                        """,
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "AI disabled behavior test request");
            ps.setLong(2, applicantId);
            ps.executeUpdate();
            try (var keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    @Test
    void suggestClassification_ShouldReturn503_WhenAiIsDisabled() throws Exception {
        mockMvc.perform(post("/api/v1/ai/suggest-classification")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Necesito ayuda con mi matrícula académica actual"
                                }
                                """))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void summarize_ShouldReturn503_WhenAiIsDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/ai/summarize/" + summarizeRequestId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void actuatorHealth_ShouldReturn200_WhenAiIsDisabled() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
