package co.edu.uniquindio.triage.application.port.out.persistence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestSearchCriteriaTest {

    private static RequestSearchCriteria criteria(
            int page, int size, String sort) {
        return new RequestSearchCriteria(null, null, null, null, null, null, null, page, size, sort);
    }

    @Test
    void nullFiltersBecomeEmptyOptionals() {
        var c = criteria(0, 20, "field,asc");

        assertThat(c.status()).isEmpty();
        assertThat(c.requestTypeId()).isEmpty();
        assertThat(c.priority()).isEmpty();
        assertThat(c.assignedToUserId()).isEmpty();
        assertThat(c.requesterUserId()).isEmpty();
        assertThat(c.dateFrom()).isEmpty();
        assertThat(c.dateTo()).isEmpty();
    }

    @Test
    void pageMustNotBeNegative() {
        assertThatThrownBy(() -> criteria(-1, 10, "a,b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("página");
    }

    @Test
    void sizeMustBePositive() {
        assertThatThrownBy(() -> criteria(0, 0, "a,b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamaño");
    }

    @Test
    void sortMustNotBeNullOrBlank() {
        assertThatThrownBy(() -> criteria(0, 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sort");

        assertThatThrownBy(() -> criteria(0, 10, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sort");
    }

    @Test
    void sortIsTrimmed() {
        var c = criteria(0, 5, "  username,desc  ");
        assertThat(c.sort()).isEqualTo("username,desc");
    }
}
