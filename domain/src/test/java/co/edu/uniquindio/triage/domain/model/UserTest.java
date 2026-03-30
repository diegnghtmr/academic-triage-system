package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.UserNotActiveException;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void anonymousRegistrationRoleMustDefaultToStudent() {
        var resolvedRole = User.resolveRegistrationRole(Role.ADMIN, null);

        assertThat(resolvedRole).isEqualTo(Role.STUDENT);
    }

    @Test
    void adminCanAssignElevatedRoleOnRegistration() {
        var resolvedRole = User.resolveRegistrationRole(Role.STAFF, Role.ADMIN);

        assertThat(resolvedRole).isEqualTo(Role.STAFF);
    }

    @Test
    void inactiveUserMustFailActiveGuard() {
        var user = User.reconstitute(
                new UserId(10L),
                new Username("jperez"),
                "Juan",
                "Pérez",
                new PasswordHash("hash-value"),
                new Identification("1094123456"),
                new Email("jperez@uniquindio.edu.co"),
                Role.STUDENT,
                false
        );

        assertThatThrownBy(user::ensureActive)
                .isInstanceOf(UserNotActiveException.class);
    }

    @Test
    void shouldUpdateProfileWhenValidDataProvided() {
        var user = createUser();
        var newFirstName = "Nuevo Nombre";
        var newLastName = "Nuevo Apellido";
        var newIdentification = new Identification("1234567890");

        user.updateProfile(newFirstName, newLastName, newIdentification);

        assertThat(user.getFirstName()).isEqualTo(newFirstName);
        assertThat(user.getLastName()).isEqualTo(newLastName);
        assertThat(user.getIdentification()).isEqualTo(newIdentification);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingProfileWithInvalidNames() {
        var user = createUser();
        var newIdentification = new Identification("1234567890");

        assertThatThrownBy(() -> user.updateProfile("", "Apellido", newIdentification))
                .isInstanceOf(IllegalArgumentException.class);
        
        assertThatThrownBy(() -> user.updateProfile("Nombre", null, newIdentification))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldDeactivateAndActivateUser() {
        var user = createUser();
        assertThat(user.isActive()).isTrue();

        user.deactivate();
        assertThat(user.isActive()).isFalse();

        user.activate();
        assertThat(user.isActive()).isTrue();
    }

    private User createUser() {
        return User.reconstitute(
                new UserId(1L),
                new Username("user1"),
                "Nombre",
                "Apellido",
                new PasswordHash("hash"),
                new Identification("111111"),
                new Email("user1@test.com"),
                Role.STUDENT,
                true
        );
    }
}
