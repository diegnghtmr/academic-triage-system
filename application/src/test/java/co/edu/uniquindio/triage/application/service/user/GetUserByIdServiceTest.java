package co.edu.uniquindio.triage.application.service.user;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetUserByIdServiceTest {

    @Mock
    private LoadUserAuthPort loadUserAuthPort;

    private GetUserByIdService service;

    @BeforeEach
    void setUp() {
        service = new GetUserByIdService(loadUserAuthPort);
    }

    @Test
    @DisplayName("Debe retornar el usuario cuando el actor es el mismo usuario (Self-access)")
    void shouldReturnUser_whenSelfAccess() {
        // Arrange
        var userId = new UserId(1L);
        var actor = new AuthenticatedActor(userId, "jperez", Role.STUDENT);
        var expectedUser = persistedUser(1L, "jperez", Role.STUDENT, true);

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.of(expectedUser));

        // Act
        var result = service.execute(userId, actor);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(expectedUser);
        verify(loadUserAuthPort).loadById(userId);
    }

    @Test
    @DisplayName("Debe retornar el usuario cuando el actor es ADMIN")
    void shouldReturnUser_whenAdminAccess() {
        // Arrange
        var userId = new UserId(1L);
        var actor = new AuthenticatedActor(new UserId(99L), "admin", Role.ADMIN);
        var expectedUser = persistedUser(1L, "jperez", Role.STUDENT, true);

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.of(expectedUser));

        // Act
        var result = service.execute(userId, actor);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(expectedUser);
        verify(loadUserAuthPort).loadById(userId);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando un usuario intenta acceder al perfil de otro")
    void shouldThrowException_whenAccessingOtherProfile() {
        // Arrange
        var userId = new UserId(1L);
        var actor = new AuthenticatedActor(new UserId(2L), "other", Role.STAFF);

        // Act & Assert
        assertThatThrownBy(() -> service.execute(userId, actor))
                .isInstanceOf(UnauthorizedOperationException.class);

        verifyNoInteractions(loadUserAuthPort);
    }

    @Test
    @DisplayName("Debe retornar Optional vacío cuando el usuario no existe y el acceso es permitido")
    void shouldReturnEmpty_whenUserNotFound() {
        // Arrange
        var userId = new UserId(999L);
        var actor = new AuthenticatedActor(new UserId(99L), "admin", Role.ADMIN);

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.empty());

        // Act
        var result = service.execute(userId, actor);

        // Assert
        assertThat(result).isEmpty();
        verify(loadUserAuthPort).loadById(userId);
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
