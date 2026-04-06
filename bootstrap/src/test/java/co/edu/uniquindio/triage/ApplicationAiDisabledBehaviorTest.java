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

import java.nio.charset.StandardCharsets;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private javax.sql.DataSource dataSource;

    private String staffToken;

    @BeforeEach
    void setup() throws Exception {
        String secret = "12345678901234567890123456789012";
        var secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        staffToken = Jwts.builder()
                .subject("staff_user")
                .claim("uid", 1L)
                .claim("role", "STAFF")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(secretKey)
                .compact();

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
             // Ensure there is at least one channel
             stmt.execute("INSERT IGNORE INTO origin_channels (id, name, active) VALUES (1, 'Ventanilla', true)");
             // Insert a request to avoid 404
             stmt.execute("INSERT INTO academic_requests (id, description, status, registration_date, applicant_id, origin_channel_id, request_type_id) " +
                          "VALUES (1, 'test description', 'IN_PROGRESS', NOW(), (SELECT id FROM users LIMIT 1), 1, 1) " +
                          "ON DUPLICATE KEY UPDATE id=id");
        }
    }

    @Test
    void suggestClassification_ShouldReturn503_WhenAiIsDisabled() throws Exception {
        mockMvc.perform(post("/api/v1/ai/suggest-classification")
                .header("Authorization", "Bearer " + staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "description": "Necesito ayuda con mi matrícula"
                        }
                        """))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void summarize_ShouldReturn503_WhenAiIsDisabled() throws Exception {
        mockMvc.perform(get("/api/v1/ai/summarize/1")
                .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void actuatorHealth_ShouldReturn200_WhenAiIsDisabled() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}

