package co.edu.uniquindio.triage.infrastructure.adapter.out.security;

import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenAdapterTest {

    private static final String SECRET = "12345678901234567890123456789012";

    @Test
    void issueAndParseMustPreserveSubjectUidAndRoleClaims() {
        var adapter = new JwtTokenAdapter(SECRET, 3_600_000);

        var issued = adapter.issue(sampleUser(Role.ADMIN));
        var parsed = adapter.parse(issued.token());

        assertThat(issued.tokenType()).isEqualTo("Bearer");
        assertThat(issued.expiresIn()).isEqualTo(3600);
        assertThat(parsed.userId()).isEqualTo(1L);
        assertThat(parsed.username()).isEqualTo("jperez");
        assertThat(parsed.role()).isEqualTo(Role.ADMIN);

        var claims = Jwts.parser()
                .verifyWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(issued.token())
                .getPayload();

        assertThat(claims.keySet()).containsExactlyInAnyOrder("sub", "uid", "role", "iat", "exp");
    }

    private User sampleUser(Role role) {
        return User.reconstitute(
                new UserId(1L),
                new Username("jperez"),
                "Juan Pérez",
                new PasswordHash("hash-value"),
                new Identification("1094123456"),
                new Email("jperez@uniquindio.edu.co"),
                role,
                true
        );
    }
}
