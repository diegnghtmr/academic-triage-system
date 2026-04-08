package co.edu.uniquindio.triage.application.port.out.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthTokenTest {

    @Test
    void tokenMustNotBeNullOrBlank() {
        assertThatThrownBy(() -> new AuthToken(null, "Bearer", 60L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("token");

        assertThatThrownBy(() -> new AuthToken("  ", "Bearer", 60L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tokenTypeMustNotBeNullOrBlank() {
        assertThatThrownBy(() -> new AuthToken("abc", null, 60L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipo");

        assertThatThrownBy(() -> new AuthToken("abc", "\n", 60L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiresInMustBePositive() {
        assertThatThrownBy(() -> new AuthToken("abc", "Bearer", 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expiración");

        assertThatThrownBy(() -> new AuthToken("abc", "Bearer", -5L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tokenAndTypeAreTrimmed() {
        var token = new AuthToken("  access-value  ", "  Bearer  ", 120L);
        assertThat(token.token()).isEqualTo("access-value");
        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresIn()).isEqualTo(120L);
    }
}
