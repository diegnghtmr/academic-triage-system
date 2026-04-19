package co.edu.uniquindio.triage;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.MeterRegistry;
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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class ApplicationIdempotencyE2ETest {

    private static final String SECRET = "12345678901234567890123456789012";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MeterRegistry meterRegistry;

    private long adminId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        try (var connection = dataSource.getConnection()) {
            // Delete user if exists to avoid conflicts
            try (var stmt = connection.createStatement()) {
                stmt.execute("DELETE FROM users WHERE username = 'admin-e2e'");
            }
            try (var stmt = connection.prepareStatement(
                    "INSERT INTO users (username, email, identification, first_name, last_name, role, active, password_hash) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, "admin-e2e");
                stmt.setString(2, "admin-e2e@uniquindio.edu.co");
                stmt.setString(3, "99999999");
                stmt.setString(4, "Admin");
                stmt.setString(5, "E2E");
                stmt.setString(6, "ADMIN");
                stmt.setBoolean(7, true);
                stmt.setString(8, "hash");
                stmt.executeUpdate();
                try (var rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        adminId = rs.getLong(1);
                    }
                }
            }
        }
    }

    @Test
    void postCreateRequestWithRealRetryMustReturnReplayed() throws Exception {
        var idempotencyKey = UUID.randomUUID().toString();
        var payload = """
                {
                  "name": "Type %s",
                  "description": "Desc",
                  "isActive": true
                }
                """.formatted(idempotencyKey);

        // 1. Fresh Request
        mockMvc.perform(post("/api/v1/catalogs/request-types")
                        .header("Authorization", "Bearer " + adminToken())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Status", "fresh"));

        // 2. Retry Request (Replay)
        mockMvc.perform(post("/api/v1/catalogs/request-types")
                        .header("Authorization", "Bearer " + adminToken())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Status", "replayed"));

        // Verify it was only created once
        try (var connection = dataSource.getConnection();
             var stmt = connection.prepareStatement("SELECT COUNT(*) FROM request_types WHERE name = ?")) {
            stmt.setString(1, "Type " + idempotencyKey);
            try (var rs = stmt.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1); // Only 1 record
            }
        }
        
        assertThat(meterRegistry.counter("idempotency.replays.total").count()).isGreaterThan(0);
    }

    @Test
    void requestMismatchMustReturn422() throws Exception {
        var idempotencyKey = UUID.randomUUID().toString();
        var payload1 = """
                {
                  "name": "Type %s",
                  "description": "Desc",
                  "isActive": true
                }
                """.formatted(idempotencyKey);
                
        var payload2 = """
                {
                  "name": "Type %s MODIFIED",
                  "description": "Desc",
                  "isActive": true
                }
                """.formatted(idempotencyKey);

        // 1. Fresh Request
        mockMvc.perform(post("/api/v1/catalogs/request-types")
                        .header("Authorization", "Bearer " + adminToken())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload1))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Status", "fresh"));

        // 2. Retry Request with different payload (Mismatch)
        mockMvc.perform(post("/api/v1/catalogs/request-types")
                        .header("Authorization", "Bearer " + adminToken())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload2))
                .andExpect(status().isUnprocessableEntity());

        assertThat(meterRegistry.counter("idempotency.mismatches.total").count()).isGreaterThan(0);
    }

    @Test
    void outstandingRequestMustReturn409() throws Exception {
        var idempotencyKey = UUID.randomUUID().toString();
        var payload = """
                {
                  "name": "Outstanding %s",
                  "description": "Desc",
                  "isActive": true
                }
                """.formatted(idempotencyKey);

        // 1. Create a fresh request first so it computes the real fingerprint and stores it.
        mockMvc.perform(post("/api/v1/catalogs/request-types")
                        .header("Authorization", "Bearer " + adminToken())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        // 2. Hack the database to set it back to PROCESSING (simulating in-flight)
        try (var connection = dataSource.getConnection();
             var stmt = connection.prepareStatement(
                     "UPDATE idempotency_keys SET status = 'PROCESSING' WHERE idempotency_key = ?")) {
            stmt.setString(1, idempotencyKey);
            stmt.executeUpdate();
        }

        // 3. Send request again, should collide, match fingerprint, and return 409
        mockMvc.perform(post("/api/v1/catalogs/request-types")
                        .header("Authorization", "Bearer " + adminToken())
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict()); // 409

        assertThat(meterRegistry.counter("idempotency.outstanding.total").count()).isGreaterThan(0);
    }

    private String adminToken() {
        var secretKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("admin-e2e")
                .claim("uid", adminId)
                .claim("role", "ADMIN")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(secretKey)
                .compact();
    }
}
