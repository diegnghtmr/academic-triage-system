package co.edu.uniquindio.triage.application.service.user;

import co.edu.uniquindio.triage.application.port.in.common.Page;
import co.edu.uniquindio.triage.application.port.in.user.command.GetUsersQueryModel;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUsersPort;
import co.edu.uniquindio.triage.application.port.out.persistence.UserSearchCriteria;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUsersServiceTest {

    @Mock
    private LoadUsersPort loadUsersPort;

    @InjectMocks
    private GetUsersService service;

    @Test
    @DisplayName("Debe retornar una página de usuarios exitosamente")
    void shouldReturnPageOfUsers_whenExecuted() {
        // Arrange
        var user1 = persistedUser(1L, "jperez", Role.STUDENT, true);
        var user2 = persistedUser(2L, "mlopez", Role.STAFF, true);
        var expectedPage = new Page<>(List.of(user1, user2), 2L, 1, 0, 10);
        var query = new GetUsersQueryModel(Optional.empty(), Optional.empty(), 0, 10, "username,asc");

        when(loadUsersPort.loadAll(any(UserSearchCriteria.class))).thenReturn(expectedPage);

        // Act
        var result = service.execute(query);

        // Assert
        assertThat(result).isSameAs(expectedPage);
        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Debe delegar correctamente los criterios de búsqueda al LoadUsersPort")
    void shouldDelegateCorrectCriteria_whenExecuted() {
        // Arrange
        var query = new GetUsersQueryModel(
                Optional.of(Role.STUDENT),
                Optional.of(true),
                2,
                25,
                "email,desc"
        );
        var emptyPage = new Page<User>(List.of(), 0L, 0, 2, 25);

        when(loadUsersPort.loadAll(any(UserSearchCriteria.class))).thenReturn(emptyPage);

        // Act
        service.execute(query);

        // Assert
        var criteriaCaptor = ArgumentCaptor.forClass(UserSearchCriteria.class);
        verify(loadUsersPort).loadAll(criteriaCaptor.capture());

        var captured = criteriaCaptor.getValue();
        assertThat(captured.role()).contains(Role.STUDENT);
        assertThat(captured.active()).contains(true);
        assertThat(captured.page()).isEqualTo(2);
        assertThat(captured.size()).isEqualTo(25);
        assertThat(captured.sort()).isEqualTo("email,desc");
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
