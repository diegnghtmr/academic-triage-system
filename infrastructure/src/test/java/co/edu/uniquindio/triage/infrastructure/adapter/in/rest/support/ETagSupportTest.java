package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support;

import co.edu.uniquindio.triage.application.exception.ETagMismatchException;
import co.edu.uniquindio.triage.application.exception.MissingIfMatchPreconditionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ETagSupportTest {

    private final ETagSupport support = new ETagSupport();

    @Test
    void toETagValueMustWrapVersionInQuotes() {
        assertThat(support.toETagValue(7L)).isEqualTo("\"7\"");
        assertThat(support.toETagValue(0L)).isEqualTo("\"0\"");
    }

    @Test
    void parseIfMatchMustReturnVersionForStrongETag() {
        assertThat(support.parseIfMatch("\"5\"")).isEqualTo(5L);
        assertThat(support.parseIfMatch("\"0\"")).isEqualTo(0L);
        assertThat(support.parseIfMatch("\"100\"")).isEqualTo(100L);
    }

    @Test
    void parseIfMatchMustThrowMissingPreconditionWhenHeaderIsNull() {
        assertThatThrownBy(() -> support.parseIfMatch(null))
                .isInstanceOf(MissingIfMatchPreconditionException.class);
    }

    @Test
    void parseIfMatchMustThrowMissingPreconditionWhenHeaderIsBlank() {
        assertThatThrownBy(() -> support.parseIfMatch("   "))
                .isInstanceOf(MissingIfMatchPreconditionException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"W/\"5\"", "W/\"0\""})
    void parseIfMatchMustRejectWeakETags(String weakETag) {
        assertThatThrownBy(() -> support.parseIfMatch(weakETag))
                .isInstanceOf(ETagMismatchException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"5", "abc", "\"abc\"", "\"\""})
    void parseIfMatchMustRejectInvalidFormats(String invalid) {
        assertThatThrownBy(() -> support.parseIfMatch(invalid))
                .isInstanceOf(ETagMismatchException.class);
    }

    @Test
    void parseIfMatchMustHandleWhitespace() {
        assertThat(support.parseIfMatch("  \"3\"  ")).isEqualTo(3L);
    }
}
