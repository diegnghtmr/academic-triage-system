package co.edu.uniquindio.triage.application.port.in.command.request;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListRequestsQueryModelTest {

    @Test
    void nullOptionalsBecomeEmpty() {
        var model = new ListRequestsQueryModel(null, null, null, null, null, null, null, 0, 50, "status,asc");

        assertThat(model.status()).isEmpty();
        assertThat(model.requestTypeId()).isEmpty();
        assertThat(model.priority()).isEmpty();
        assertThat(model.assignedToUserId()).isEmpty();
        assertThat(model.requesterUserId()).isEmpty();
        assertThat(model.dateFrom()).isEmpty();
        assertThat(model.dateTo()).isEmpty();
    }

    @Test
    void sortNullOrBlankYieldsDefaultRegistrationSort() {
        var withNullSort = new ListRequestsQueryModel(null, null, null, null, null, null, null, 0, 10, null);
        assertThat(withNullSort.sort()).isEqualTo("registrationDateTime,desc");

        var withBlankSort = new ListRequestsQueryModel(null, null, null, null, null, null, null, 0, 10, "   ");
        assertThat(withBlankSort.sort()).isEqualTo("registrationDateTime,desc");
    }

    @Test
    void sortWithoutCommaIsRejected() {
        assertThatThrownBy(() -> new ListRequestsQueryModel(null, null, null, null, null, null, null, 0, 10, "deadlineasc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sort");
    }

    @Test
    void pageMustNotBeNegative() {
        assertThatThrownBy(() -> new ListRequestsQueryModel(null, null, null, null, null, null, null, -1, 10, "id,asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("página");
    }

    @Test
    void pageSizeMustBeBetweenOneAndOneHundred() {
        assertThatThrownBy(() -> new ListRequestsQueryModel(null, null, null, null, null, null, null, 0, 0, "id,asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamaño");

        assertThatThrownBy(() -> new ListRequestsQueryModel(null, null, null, null, null, null, null, 0, 101, "id,asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamaño");
    }

    @Test
    void dateFromMustNotBeAfterDateTo() {
        assertThatThrownBy(() -> new ListRequestsQueryModel(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(LocalDate.of(2026, 5, 1)),
                Optional.of(LocalDate.of(2026, 4, 1)),
                0,
                20,
                "registrationDateTime,desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateFrom");
    }

    @Test
    void sortIsTrimmedWhenValid() {
        var model = new ListRequestsQueryModel(null, null, null, null, null, null, null, 0, 15, "  deadline,asc  ");
        assertThat(model.sort()).isEqualTo("deadline,asc");
    }
}
