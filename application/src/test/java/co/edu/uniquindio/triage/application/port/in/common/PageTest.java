package co.edu.uniquindio.triage.application.port.in.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageTest {

    @Test
    void nullContentBecomesEmptyList() {
        var page = new Page<String>(null, 0L, 0, 0, 1);
        assertThat(page.content()).isEmpty();
    }

    @Test
    void contentIsDefensiveCopyAndImmutable() {
        var backing = new ArrayList<String>();
        backing.add("a");
        var page = new Page<>(backing, 1L, 1, 0, 1);
        backing.add("b");
        assertThat(page.content()).containsExactly("a");
        assertThatThrownBy(() -> page.content().add("c"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void totalElementsMustNotBeNegative() {
        assertThatThrownBy(() -> new Page<>(List.of(), -1L, 0, 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("elementos");
    }

    @Test
    void totalPagesMustNotBeNegative() {
        assertThatThrownBy(() -> new Page<>(List.of(), 0L, -1, 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("páginas");
    }

    @Test
    void currentPageMustNotBeNegative() {
        assertThatThrownBy(() -> new Page<>(List.of(), 0L, 0, -1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actual");
    }

    @Test
    void pageSizeMustBePositive() {
        assertThatThrownBy(() -> new Page<>(List.of(), 0L, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamaño");
    }
}
