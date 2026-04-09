package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.UserNotActiveException;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private static User user(long id, String username, Role role) {
        return User.reconstitute(
                new UserId(id),
                new Username(username),
                "Nombre",
                "Apellido",
                new PasswordHash("h"),
                new Identification(String.format("%010d", id)),
                new Email(username + "@uniquindio.edu.co"),
                role,
                true);
    }

    private static User student(long id, String username) {
        return user(id, username, Role.STUDENT);
    }

    private static User staff(long id, String username) {
        return user(id, username, Role.STAFF);
    }

    private static User admin(long id, String username) {
        return user(id, username, Role.ADMIN);
    }

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

    @Test
    void shouldReturnFullName() {
        var user = createUser();
        assertThat(user.getFullName()).isEqualTo("Nombre Apellido");
    }

    @Test
    void shouldValidateEqualityBasedOnId() {
        var id1 = new UserId(1L);
        var id2 = new UserId(2L);
        
        var user1 = User.reconstitute(id1, new Username("user123"), "Nombre", "Apellido", new PasswordHash("hashvalue"), new Identification("12345678"), new Email("email@test.com"), Role.STUDENT, true);
        var user2 = User.reconstitute(id1, new Username("user456"), "Otro", "Otro", new PasswordHash("otherhash"), new Identification("87654321"), new Email("other@test.com"), Role.STAFF, false);
        var user3 = User.reconstitute(id2, new Username("user123"), "Nombre", "Apellido", new PasswordHash("hashvalue"), new Identification("12345678"), new Email("email@test.com"), Role.STUDENT, true);

        assertThat(user1).isEqualTo(user2);
        assertThat(user1).isNotEqualTo(user3);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    void shouldResolveRoleCorrect() {
        // Staff/Admin cannot be assigned by non-admin
        assertThat(User.resolveRegistrationRole(Role.STAFF, Role.STAFF)).isEqualTo(Role.STUDENT);
        assertThat(User.resolveRegistrationRole(Role.ADMIN, Role.STUDENT)).isEqualTo(Role.STUDENT);
        
        // Admin can assign anything
        assertThat(User.resolveRegistrationRole(Role.STAFF, Role.ADMIN)).isEqualTo(Role.STAFF);
        assertThat(User.resolveRegistrationRole(Role.ADMIN, Role.ADMIN)).isEqualTo(Role.ADMIN);
        
        // Student is default
        assertThat(User.resolveRegistrationRole(null, Role.ADMIN)).isEqualTo(Role.STUDENT);
    }

    @Test
    void shouldUpdateAuthData() {
        var user = createUser();
        var newEmail = new Email("new@test.com");
        var newRole = Role.STAFF;
        
        user.updateEmail(newEmail);
        user.updateRole(newRole);
        
        assertThat(user.getEmail()).isEqualTo(newEmail);
        assertThat(user.getRole()).isEqualTo(newRole);
    }

    @Test
    void canCancelRequestAllowsStudentOwnerStaffAndAdmin() {
        var applicantId = new UserId(100L);
        var owner = student(100L, "owner");
        var otherStudent = student(200L, "other");
        var transientStudent = User.registerNew(
                new Username("newstudent"),
                "Nombre",
                "Apellido",
                new PasswordHash("h"),
                new Identification("0000000300"),
                new Email("newstudent@uniquindio.edu.co"),
                Role.STUDENT);

        assertThat(owner.canCancelRequest(applicantId)).isTrue();
        assertThat(otherStudent.canCancelRequest(applicantId)).isFalse();
        assertThat(transientStudent.canCancelRequest(applicantId)).isFalse();
        assertThat(staff(300L, "staff1").canCancelRequest(applicantId)).isTrue();
        assertThat(admin(400L, "admin1").canCancelRequest(applicantId)).isTrue();
    }

    @Test
    void canClassifyRequestIsLimitedToStaffAndAdmin() {
        assertThat(staff(1L, "staff-user").canClassifyRequest()).isTrue();
        assertThat(admin(2L, "admin-user").canClassifyRequest()).isTrue();
        assertThat(student(3L, "student-usr").canClassifyRequest()).isFalse();
    }

    @Test
    void equalsHandlesNullForeignTypeAndTransientUsers() {
        var persisted = createUser();
        assertThat(persisted.equals(persisted)).isTrue();
        assertThat(persisted.equals(null)).isFalse();
        assertThat(persisted.equals(Integer.valueOf(1))).isFalse();

        var a = User.registerNew(
                new Username("user-a"),
                "N",
                "L",
                new PasswordHash("h"),
                new Identification("9"),
                new Email("u1@test.com"),
                Role.STUDENT);
        var b = User.registerNew(
                new Username("user-b"),
                "N",
                "L",
                new PasswordHash("h"),
                new Identification("9"),
                new Email("u2@test.com"),
                Role.STUDENT);
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void updateProfileMustValidateNameLengthAndBlanks() {
        var user = createUser();
        var id = new Identification("1234567890");

        assertThatThrownBy(() -> user.updateProfile(" ", "Apellido", id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        assertThatThrownBy(() -> user.updateProfile("Nombre", "   ", id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("apellido");

        var longName = "x".repeat(76);
        assertThatThrownBy(() -> user.updateProfile(longName, "Apellido", id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombre");

        assertThatThrownBy(() -> user.updateProfile("Nombre", "y".repeat(76), id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("apellido");
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
