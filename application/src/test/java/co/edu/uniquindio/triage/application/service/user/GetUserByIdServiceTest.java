package co.edu.uniquindio.triage.application.service.user;

import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.domain.enums.Role;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserByIdServiceTest {

    @Mock
    private LoadUserAuthPort loadUserAuthPort;

    @InjectMocks
    private GetUserByIdService service;

    @Test
    @DisplayName("Debe retornar el usuario cuando existe")
    void shouldReturnUser_whenUserExists() {
        // Arrange
        var userId = new UserId(1L);
        var expectedUser = persistedUser(1L, "jperez", Role.STUDENT, true);

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.of(expectedUser));

        // Act
        var result = service.execute(userId);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(expectedUser);
        verify(loadUserAuthPort).loadById(userId);
    }

    @Test
    @DisplayName("Debe retornar Optional vacío cuando el usuario no existe")
    void shouldReturnEmpty_whenUserNotFound() {
        // Arrange
        var userId = new UserId(999L);

        when(loadUserAuthPort.loadById(userId)).thenReturn(Optional.empty());

        // Act
        var result = service.execute(userId);

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
