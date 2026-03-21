package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.util.Objects;

public class User {
    private final UserId id;
    private final String identification;
    private final String firstName;
    private final String lastName;
    private Email email;
    private Role role;
    private boolean active;

    public User(UserId id, String identification, String firstName, String lastName,
                Email email, Role role, boolean active) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
        this.identification = validateIdentification(identification);
        this.firstName = validateName(firstName, "El nombre");
        this.lastName = validateName(lastName, "El apellido");
        this.email = Objects.requireNonNull(email, "El email no puede ser null");
        this.role = Objects.requireNonNull(role, "El rol no puede ser null");
        this.active = active;
    }

    public static User reconstitute(UserId id, String identification, String firstName, String lastName,
                             Email email, Role role, boolean active) {
        return new User(id, identification, firstName, lastName, email, role, active);
    }

    private String validateIdentification(String identification) {
        if (identification == null || identification.isBlank()) {
            throw new IllegalArgumentException("La identificación no puede ser null o vacía");
        }
        String trimmed = identification.trim();
        if (trimmed.length() > 20) {
            throw new IllegalArgumentException("La identificación no puede tener más de 20 caracteres");
        }
        return trimmed;
    }

    private String validateName(String name, String fieldName) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(fieldName + " no puede ser null o vacío");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException(fieldName + " no puede tener más de 100 caracteres");
        }
        return trimmed;
    }

    public UserId getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getIdentification() {
        return identification;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public void updateRole(Role newRole) {
        this.role = Objects.requireNonNull(newRole, "El rol no puede ser null");
    }

    public void updateEmail(Email newEmail) {
        this.email = Objects.requireNonNull(newEmail, "El email no puede ser null");
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isStaff() {
        return role == Role.STAFF;
    }

    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    public boolean canCancelRequest(UserId applicantId) {
        return this.isStudent() && this.id.equals(applicantId);
    }

    public boolean canClassifyRequest() {
        return this.isStaff() || this.isAdmin();
    }

    public boolean canRejectRequest() {
        return this.isAdmin();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", identification='" + identification + '\'' +
                ", email=" + email +
                ", role=" + role +
                ", active=" + active +
                '}';
    }
}
