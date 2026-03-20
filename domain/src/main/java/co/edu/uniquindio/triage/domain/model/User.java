package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.RoleEnum;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.domain.model.vo.UserEmail;

import java.util.Objects;

public class User {
    private UserId id;
    private String username;
    private UserEmail email;
    private String identification;
    private String firstName;
    private String lastName;
    private RoleEnum role;
    private boolean active;
    private String password;

    public User(UserId id, String username, UserEmail email, String identification,
                String firstName, String lastName, RoleEnum role, boolean active, String password) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
        this.username = validateUsername(username);
        this.email = Objects.requireNonNull(email, "El email no puede ser null");
        this.identification = validateIdentification(identification);
        this.firstName = validateName(firstName, "El nombre");
        this.lastName = validateName(lastName, "El apellido");
        this.role = Objects.requireNonNull(role, "El rol no puede ser null");
        this.active = active;
        this.password = validatePassword(password);
    }

    private String validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El username no puede ser null o vacío");
        }
        String trimmed = username.trim();
        if (trimmed.length() < 3 || trimmed.length() > 50) {
            throw new IllegalArgumentException("El username debe tener entre 3 y 50 caracteres");
        }
        return trimmed;
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

    private String validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede ser null o vacía");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        return password;
    }

    public UserId getId() {
        return id;
    }

    public void setId(UserId id) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
    }

    public String getUsername() {
        return username;
    }

    public UserEmail getEmail() {
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

    public RoleEnum getRole() {
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

    public String getPassword() {
        return password;
    }

    public void updatePassword(String newPassword) {
        this.password = validatePassword(newPassword);
    }

    public boolean isAdmin() {
        return role == RoleEnum.ADMIN;
    }

    public boolean isStaff() {
        return role == RoleEnum.STAFF;
    }

    public boolean isStudent() {
        return role == RoleEnum.STUDENT;
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
                ", username='" + username + '\'' +
                ", email=" + email +
                ", role=" + role +
                ", active=" + active +
                '}';
    }
}
