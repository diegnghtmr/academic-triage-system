package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.UserNotActiveException;
import co.edu.uniquindio.triage.domain.model.id.UserId;

import java.util.Objects;

public class User {
    private final UserId id;
    private final Username username;
    private final String fullName;
    private final PasswordHash passwordHash;
    private final Identification identification;
    private Email email;
    private Role role;
    private boolean active;

    private User(UserId id, Username username, String fullName, PasswordHash passwordHash,
                 Identification identification, Email email, Role role, boolean active) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "El username no puede ser null");
        this.fullName = validateFullName(fullName);
        this.passwordHash = Objects.requireNonNull(passwordHash, "El password hash no puede ser null");
        this.identification = Objects.requireNonNull(identification, "La identificación no puede ser null");
        this.email = Objects.requireNonNull(email, "El email no puede ser null");
        this.role = Objects.requireNonNull(role, "El rol no puede ser null");
        this.active = active;
    }

    public static User registerNew(Username username, String fullName, PasswordHash passwordHash,
                                   Identification identification, Email email, Role role) {
        return new User(null, username, fullName, passwordHash, identification, email, role, true);
    }

    public static User reconstitute(UserId id, Username username, String fullName, PasswordHash passwordHash,
                                    Identification identification, Email email, Role role, boolean active) {
        return new User(Objects.requireNonNull(id, "El id no puede ser null"), username, fullName,
                passwordHash, identification, email, role, active);
    }

    public static Role resolveRegistrationRole(Role requestedRole, Role actorRole) {
        if (actorRole == Role.ADMIN && requestedRole != null) {
            return requestedRole;
        }
        return Role.STUDENT;
    }

    private String validateFullName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El nombre completo no puede ser null o vacío");
        }
        var trimmed = value.trim();
        if (trimmed.length() > 150) {
            throw new IllegalArgumentException("El nombre completo no puede tener más de 150 caracteres");
        }
        return trimmed;
    }

    public UserId getId() {
        return id;
    }

    public Username getUsername() {
        return username;
    }

    public Email getEmail() {
        return email;
    }

    public Identification getIdentification() {
        return identification;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public void ensureActive() {
        if (!active) {
            throw new UserNotActiveException(id);
        }
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
        return (this.isStudent() && this.id.equals(applicantId)) || this.isStaff() || this.isAdmin();
    }

    public boolean canClassifyRequest() {
        return this.isStaff() || this.isAdmin();
    }

    public boolean canRejectRequest() {
        return this.isAdmin();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var user = (User) o;
        return id != null && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username=" + username +
                ", identification=" + identification +
                ", email=" + email +
                ", role=" + role +
                ", active=" + active +
                '}';
    }
}
