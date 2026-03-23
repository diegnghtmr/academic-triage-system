package co.edu.uniquindio.triage.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthValueObjectsTest {

    @Test
    void usernameMustRespectMinimumLength() {
        assertThatThrownBy(() -> new Username("ab"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos 3 caracteres");
    }

    @Test
    void identificationMustRejectValuesLongerThanContract() {
        assertThatThrownBy(() -> new Identification("123456789012345678901"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("más de 20 caracteres");
    }

    @Test
    void passwordHashCannotBeBlank() {
        assertThatThrownBy(() -> new PasswordHash("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password hash");
    }
}
