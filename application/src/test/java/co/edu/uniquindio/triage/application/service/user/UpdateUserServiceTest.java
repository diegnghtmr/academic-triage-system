package co.edu.uniquindio.triage.application.service.user;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.user.command.UpdateUserCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveUserPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.BusinessRuleViolationException;
import co.edu.uniquindio.triage.domain.exception.DuplicateUserException;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserServiceTest {

    @Mock
    private LoadUserAuthPort loadUserAuthPort;

    @Mock
    private SaveUserPort saveUserPort;

    @InjectMocks
    private UpdateUserService service;

    @Test
    @DisplayName("Debe actualizar perfil (nombre, apellido, identificación) exitosamente")
    void shouldUpdateProfile_whenValidCommand() {
        // Arrange
        var userId = new UserId(1L);
        var existingUser = persistedUser(1L, "jperez", Role.STUDENT, true);
        var actor = new AuthenticatedActor(new UserId(99L), "admin.user", Role.ADMIN);
        var command = new UpdateUserCommand(
                userId, "NuevoNombre", "NuevoApellido",
                new Identification("NEW-ID-123"),
                existingUser.getEmail(),
                existingUser.getRole(),
                true
        );

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.of(existingUser));
        when(loadUserAuthPort.existsByIdentification(command.identification())).thenReturn(false);
        when(saveUserPort.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = service.execute(command, actor);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("NuevoNombre");
        assertThat(result.getLastName()).isEqualTo("NuevoApellido");
        assertThat(result.getIdentification()).isEqualTo(new Identification("NEW-ID-123"));
        verify(saveUserPort).save(existingUser);
    }

    @Test
    @DisplayName("Debe actualizar email exitosamente cuando no está duplicado")
    void shouldUpdateEmail_whenEmailIsUnique() {
        // Arrange
        var userId = new UserId(1L);
        var existingUser = persistedUser(1L, "jperez", Role.STUDENT, true);
        var actor = new AuthenticatedActor(new UserId(99L), "admin.user", Role.ADMIN);
        var newEmail = new Email("nuevo@uniquindio.edu.co");
        var command = new UpdateUserCommand(
                userId, existingUser.getFirstName(), existingUser.getLastName(),
                existingUser.getIdentification(),
                newEmail,
                existingUser.getRole(),
                true
        );

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.of(existingUser));
        when(loadUserAuthPort.existsByEmail(newEmail)).thenReturn(false);
        when(saveUserPort.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = service.execute(command, actor);

        // Assert
        assertThat(result.getEmail()).isEqualTo(newEmail);
        verify(saveUserPort).save(existingUser);
    }

    @Test
    @DisplayName("Debe cambiar rol exitosamente cuando no es auto-modificación")
    void shouldChangeRole_whenNotSelfModification() {
        // Arrange
        var userId = new UserId(1L);
        var existingUser = persistedUser(1L, "jperez", Role.STUDENT, true);
        var actor = new AuthenticatedActor(new UserId(99L), "admin.user", Role.ADMIN);
        var command = new UpdateUserCommand(
                userId, existingUser.getFirstName(), existingUser.getLastName(),
                existingUser.getIdentification(),
                existingUser.getEmail(),
                Role.STAFF,
                true
        );

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.of(existingUser));
        when(saveUserPort.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = service.execute(command, actor);

        // Assert
        assertThat(result.getRole()).isEqualTo(Role.STAFF);
        verify(saveUserPort).save(existingUser);
    }

    @Test
    @DisplayName("Debe activar/desactivar usuario exitosamente")
    void shouldToggleActiveStatus_whenNotSelfDeactivation() {
        // Arrange
        var userId = new UserId(1L);
        var existingUser = persistedUser(1L, "jperez", Role.STUDENT, true);
        var actor = new AuthenticatedActor(new UserId(99L), "admin.user", Role.ADMIN);
        var command = new UpdateUserCommand(
                userId, existingUser.getFirstName(), existingUser.getLastName(),
                existingUser.getIdentification(),
                existingUser.getEmail(),
                existingUser.getRole(),
                false
        );

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.of(existingUser));
        when(saveUserPort.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = service.execute(command, actor);

        // Assert
        assertThat(result.isActive()).isFalse();
        verify(saveUserPort).save(existingUser);
    }

    @Test
    @DisplayName("Debe lanzar EntityNotFoundException cuando el usuario no existe")
    void shouldThrowEntityNotFoundException_whenUserNotFound() {
        // Arrange
        var userId = new UserId(999L);
        var actor = new AuthenticatedActor(new UserId(99L), "admin.user", Role.ADMIN);
        var command = new UpdateUserCommand(
                userId, "Nombre", "Apellido",
                new Identification("ID-999"),
                new Email("test@uniquindio.edu.co"),
                Role.STUDENT,
                true
        );

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.execute(command, actor))
                .isInstanceOf(EntityNotFoundException.class);

        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar DuplicateUserException cuando el email ya existe")
    void shouldThrowDuplicateUserException_whenEmailAlreadyExists() {
        // Arrange
        var userId = new UserId(1L);
        var existingUser = persistedUser(1L, "jperez", Role.STUDENT, true);
        var actor = new AuthenticatedActor(new UserId(99L), "admin.user", Role.ADMIN);
        var duplicateEmail = new Email("duplicado@uniquindio.edu.co");
        var command = new UpdateUserCommand(
                userId, existingUser.getFirstName(), existingUser.getLastName(),
                existingUser.getIdentification(),
                duplicateEmail,
                existingUser.getRole(),
                true
        );

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.of(existingUser));
        when(loadUserAuthPort.existsByEmail(duplicateEmail)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.execute(command, actor))
                .isInstanceOf(DuplicateUserException.class);

        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar DuplicateUserException cuando la identificación ya existe")
    void shouldThrowDuplicateUserException_whenIdentificationAlreadyExists() {
        // Arrange
        var userId = new UserId(1L);
        var existingUser = persistedUser(1L, "jperez", Role.STUDENT, true);
        var actor = new AuthenticatedActor(new UserId(99L), "admin.user", Role.ADMIN);
        var duplicateId = new Identification("DUPLICATE-ID");
        var command = new UpdateUserCommand(
                userId, existingUser.getFirstName(), existingUser.getLastName(),
                duplicateId,
                existingUser.getEmail(),
                existingUser.getRole(),
                true
        );

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.of(existingUser));
        when(loadUserAuthPort.existsByIdentification(duplicateId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.execute(command, actor))
                .isInstanceOf(DuplicateUserException.class);

        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar BusinessRuleViolationException al intentar cambiar su propio rol")
    void shouldThrowBusinessRuleViolation_whenSelfRoleChange() {
        // Arrange
        var userId = new UserId(1L);
        var existingUser = persistedUser(1L, "jperez", Role.ADMIN, true);
        var actor = new AuthenticatedActor(userId, "jperez", Role.ADMIN);
        var command = new UpdateUserCommand(
                userId, existingUser.getFirstName(), existingUser.getLastName(),
                existingUser.getIdentification(),
                existingUser.getEmail(),
                Role.STUDENT,
                true
        );

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.of(existingUser));

        // Act & Assert
        assertThatThrownBy(() -> service.execute(command, actor))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("propio rol");

        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar BusinessRuleViolationException al intentar desactivar su propia cuenta")
    void shouldThrowBusinessRuleViolation_whenSelfDeactivation() {
        // Arrange
        var userId = new UserId(1L);
        var existingUser = persistedUser(1L, "jperez", Role.ADMIN, true);
        var actor = new AuthenticatedActor(userId, "jperez", Role.ADMIN);
        var command = new UpdateUserCommand(
                userId, existingUser.getFirstName(), existingUser.getLastName(),
                existingUser.getIdentification(),
                existingUser.getEmail(),
                existingUser.getRole(),
                false
        );

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.of(existingUser));

        // Act & Assert
        assertThatThrownBy(() -> service.execute(command, actor))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("desactivar");

        verify(saveUserPort, never()).save(any());
    }

    @Test
    @DisplayName("Debe permitir auto-actualización de perfil sin lanzar excepción")
    void shouldAllowSelfProfileUpdate_whenNotChangingRoleOrDeactivating() {
        // Arrange
        var userId = new UserId(1L);
        var existingUser = persistedUser(1L, "jperez", Role.ADMIN, true);
        var actor = new AuthenticatedActor(userId, "jperez", Role.ADMIN);
        var command = new UpdateUserCommand(
                userId, "NuevoNombre", "NuevoApellido",
                existingUser.getIdentification(),
                existingUser.getEmail(),
                existingUser.getRole(),
                true
        );

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.of(existingUser));
        when(saveUserPort.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = service.execute(command, actor);

        // Assert
        assertThat(result.getFirstName()).isEqualTo("NuevoNombre");
        assertThat(result.getLastName()).isEqualTo("NuevoApellido");
        verify(saveUserPort).save(existingUser);
    }

    private static User persistedUser(long id, String username, Role role, boolean active) {
        return User.reconstitute(
                new UserId(id),
                new Username(username),
                "Usuario",
                "Persistido",
                new PasswordHash("encoded-password"),
                new Identification("ID-" + id),
                new Email(username + "@uniquindio.edu.co"),
                role,
                active
        );
    }
}
