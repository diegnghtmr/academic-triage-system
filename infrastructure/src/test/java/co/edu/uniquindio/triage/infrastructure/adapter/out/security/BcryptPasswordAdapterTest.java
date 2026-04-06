package co.edu.uniquindio.triage.infrastructure.adapter.out.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class BcryptPasswordAdapterTest {

    private final BcryptPasswordAdapter adapter = new BcryptPasswordAdapter(new BCryptPasswordEncoder());

    @Test
    void encodeAndMatchesMustDelegateToBcryptEncoder() {
        var rawPassword = "MyPassword123";
        var encoded = adapter.encode(rawPassword);

        assertThat(encoded).isNotBlank().isNotEqualTo(rawPassword);
        assertThat(adapter.matches(rawPassword, encoded)).isTrue();
        assertThat(adapter.matches("WrongPassword123", encoded)).isFalse();
    }
}
