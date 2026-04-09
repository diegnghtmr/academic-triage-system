package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.enums.Role;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserSearchCriteriaTest {

    @Test
    void nullRoleAndActiveBecomeEmptyOptionals() {
        var criteria = new UserSearchCriteria(null, null, 0, 10, "u,v");

        assertThat(criteria.role()).isEmpty();
        assertThat(criteria.active()).isEmpty();
    }

    @Test
    void pageMustNotBeNegative() {
        assertThatThrownBy(() -> new UserSearchCriteria(Optional.empty(), Optional.empty(), -1, 5, "a,b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("página");
    }

    @Test
    void sizeMustBePositive() {
        assertThatThrownBy(() -> new UserSearchCriteria(Optional.of(Role.STAFF), Optional.empty(), 0, 0, "a,b"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamaño");
    }

    @Test
    void sortMustNotBeNullOrBlank() {
        assertThatThrownBy(() -> new UserSearchCriteria(null, null, 0, 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sort");

        assertThatThrownBy(() -> new UserSearchCriteria(null, null, 0, 1, "\t "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sort");
    }

    @Test
    void sortIsTrimmed() {
        var criteria = new UserSearchCriteria(null, null, 0, 20, "  role,desc  ");
        assertThat(criteria.sort()).isEqualTo("role,desc");
    }
}
