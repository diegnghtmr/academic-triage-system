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
import java.time.Instant;
import java.util.Date;

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
    private static final long STUDENT_ID = 200L;
    private static final String STUDENT_USERNAME = "request-smoke-student";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("DELETE FROM request_history WHERE request_id IN (SELECT id FROM academic_requests WHERE applicant_id = 200)");
            statement.execute("DELETE FROM academic_requests WHERE applicant_id = 200");
            statement.execute("DELETE FROM users WHERE id = 200");
            statement.execute("""
                    INSERT INTO users (id, username, email, identification, first_name, last_name, role, active, password_hash)
                    VALUES (200, 'request-smoke-student', 'request-smoke-student@uniquindio.edu.co', '200000000', 'Request', 'Smoke', 'STUDENT', TRUE, '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy')
                    """);
        }
    }

    @Test
    void createRequestMustPersistAndReturn201ForAuthenticatedStudent() throws Exception {
        mockMvc.perform(post("/api/v1/requests")
                        .header("Authorization", "Bearer " + studentToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestTypeId": 1,
                                  "originChannelId": 1,
                                  "description": "Necesito validar que la creación de solicitudes funcione end to end en smoke testing",
                                  "deadline": "2026-05-15"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/requests/")))
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.requester.username").value(STUDENT_USERNAME))
                .andExpect(jsonPath("$.requestType.id").value(1));
    }

    private String studentToken() {
        var secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(STUDENT_USERNAME)
                .claim("uid", STUDENT_ID)
                .claim("role", "STUDENT")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(secretKey)
                .compact();
    }
}
