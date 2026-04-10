package co.edu.uniquindio.triage.application.port.in.command.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SuggestClassificationCommandTest {

    @Test
    void trimsAndAcceptsBoundaryLength() {
        var cmd = new SuggestClassificationCommand("  " + "a".repeat(10) + "  ");
        assertThat(cmd.description()).isEqualTo("a".repeat(10));
    }

    @Test
    void rejectsShorterThan10() {
        assertThatThrownBy(() -> new SuggestClassificationCommand("123456789"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10");
    }

    @Test
    void rejectsLongerThan2000() {
        assertThatThrownBy(() -> new SuggestClassificationCommand("x".repeat(2001)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2000");
    }

    @Test
    void acceptsExactly2000Characters() {
        var cmd = new SuggestClassificationCommand("y".repeat(2000));
        assertThat(cmd.description()).hasSize(2000);
    }
}
