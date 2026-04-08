package co.edu.uniquindio.triage.application.port.in.user.command;

import co.edu.uniquindio.triage.domain.enums.Role;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetUsersQueryModelTest {

    @Test
    void nullRoleOrActiveBecomeEmptyOptionals() {
        var model = new GetUsersQueryModel(null, null, 0, 1, "email,asc");

        assertThat(model.role()).isEmpty();
        assertThat(model.active()).isEmpty();
    }

    @Test
    void sortNullOrBlankDefaultsToUsernameAsc() {
        assertThat(new GetUsersQueryModel(Optional.empty(), Optional.empty(), 0, 10, null).sort())
                .isEqualTo("username,asc");
        assertThat(new GetUsersQueryModel(Optional.empty(), Optional.empty(), 0, 10, "  ").sort())
                .isEqualTo("username,asc");
    }

    @Test
    void pageMustNotBeNegative() {
        assertThatThrownBy(() -> new GetUsersQueryModel(Optional.empty(), Optional.empty(), -1, 10, "username,asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("página");
    }

    @Test
    void pageSizeMustBeBetweenOneAndOneHundred() {
        assertThatThrownBy(() -> new GetUsersQueryModel(Optional.empty(), Optional.empty(), 0, 0, "username,asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamaño");

        assertThatThrownBy(() -> new GetUsersQueryModel(
                        Optional.of(Role.STUDENT), Optional.of(true), 0, 101, "username,asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamaño");
    }
}
