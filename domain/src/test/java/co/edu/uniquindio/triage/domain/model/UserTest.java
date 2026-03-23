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
                "Juan Pérez",
                new PasswordHash("hash-value"),
                new Identification("1094123456"),
                new Email("jperez@uniquindio.edu.co"),
                Role.STUDENT,
                false
        );

        assertThatThrownBy(user::ensureActive)
                .isInstanceOf(UserNotActiveException.class);
    }
}
