package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.common.Page;
import co.edu.uniquindio.triage.application.port.in.user.GetUserByIdQuery;
import co.edu.uniquindio.triage.application.port.in.user.GetUsersQuery;
import co.edu.uniquindio.triage.application.port.in.user.UpdateUserUseCase;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.BusinessRuleViolationException;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.security.AuthenticatedUser;
import co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {
        UserController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        UserControllerTest.TestMappersConfiguration.class,
        UserControllerTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        UserControllerTest.TestMappersConfiguration.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetUsersQuery getUsersQuery;

    @MockitoBean
    private GetUserByIdQuery getUserByIdQuery;

    @MockitoBean
    private LoadUserAuthPort loadUserAuthPort;

    @MockitoBean
    private UpdateUserUseCase updateUserUseCase;

    @Nested
    @DisplayName("GET /api/v1/users — Listar usuarios paginados")
    class GetUsers {

        @Test
        @DisplayName("Debe retornar 200 con usuarios paginados cuando el actor es ADMIN")
        void mustReturn200WithPaginatedUsersWhenAdmin() throws Exception {
            var user = sampleUser(1L, "jperez", Role.STUDENT);
            var usersPage = new Page<>(List.of(user), 1, 1, 0, 10);
            given(getUsersQuery.execute(any())).willReturn(usersPage);

            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users")
                            .with(adminAuthentication())
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "username,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].username").value("jperez"))
                    .andExpect(jsonPath("$.content[0].firstName").value("Juan"))
                    .andExpect(jsonPath("$.content[0].lastName").value("Pérez"))
                    .andExpect(jsonPath("$.content[0].role").value("STUDENT"))
                    .andExpect(jsonPath("$.content[0].active").value(true))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.currentPage").value(0))
                    .andExpect(jsonPath("$.pageSize").value(10));
        }

        @Test
        @DisplayName("Debe retornar 403 cuando el actor no tiene rol ADMIN")
        void mustReturn403WhenActorIsNotAdmin() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users")
                            .with(staffAuthentication()))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Debe retornar 403 cuando el actor tiene rol STUDENT")
        void mustReturn403WhenActorIsStudent() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users")
                            .with(studentAuthentication()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Debe retornar 401 cuando no hay autenticación")
        void mustReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.title").value("Unauthorized"));
        }

        @Test
        @DisplayName("Debe filtrar por rol y estado activo cuando se envían parámetros")
        void mustFilterByRoleAndActiveWhenParamsProvided() throws Exception {
            var user = sampleUser(2L, "staff01", Role.STAFF);
            var usersPage = new Page<>(List.of(user), 1, 1, 0, 10);
            given(getUsersQuery.execute(any())).willReturn(usersPage);

            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users")
                            .with(adminAuthentication())
                            .param("role", "STAFF")
                            .param("active", "true")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "username,asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(2))
                    .andExpect(jsonPath("$.content[0].role").value("STAFF"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id} — Obtener usuario por ID")
    class GetUserById {

        @Test
        @DisplayName("Debe retornar 200 con el usuario cuando existe y el actor es ADMIN")
        void mustReturn200WithUserWhenExistsAndAdmin() throws Exception {
            var user = sampleUser(5L, "maria", Role.STUDENT);
            given(getUserByIdQuery.execute(any(), any())).willReturn(Optional.of(user));

            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users/{id}", 5)
                            .with(adminAuthentication()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.username").value("maria"))
                    .andExpect(jsonPath("$.firstName").value("Juan"))
                    .andExpect(jsonPath("$.lastName").value("Pérez"))
                    .andExpect(jsonPath("$.email").value("maria@uniquindio.edu.co"))
                    .andExpect(jsonPath("$.role").value("STUDENT"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("Debe retornar 200 con el usuario cuando el actor accede a su propio perfil (self-access)")
        void mustReturn200WhenUserAccessesOwnProfile() throws Exception {
            var userId = 7L;
            var user = sampleUser(userId, "jperez", Role.STUDENT);
            given(getUserByIdQuery.execute(any(), any())).willReturn(Optional.of(user));

            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users/{id}", userId)
                            .with(authentication(userId, "jperez", Role.STUDENT)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.username").value("jperez"));
        }

        @Test
        @DisplayName("Debe retornar 403 cuando el actor (no admin) intenta acceder al perfil de otro usuario")
        void mustReturn403WhenUserAccessesOtherProfile() throws Exception {
            var userId = 5L;
            var otherUserId = 10L;
            
            given(getUserByIdQuery.execute(any(), any()))
                    .willThrow(new UnauthorizedOperationException(Role.STUDENT, "obtener detalle de otro usuario"));

            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users/{id}", otherUserId)
                            .with(authentication(userId, "jperez", Role.STUDENT)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Debe retornar 404 cuando el usuario no existe")
        void mustReturn404WhenUserDoesNotExist() throws Exception {
            given(getUserByIdQuery.execute(any(), any())).willReturn(Optional.empty());

            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users/{id}", 999)
                            .with(adminAuthentication()))
                    .andExpect(status().isNotFound())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value("Not Found"));
        }

        @Test
        @DisplayName("Debe retornar 403 cuando el actor no tiene rol ADMIN")
        void mustReturn403WhenActorIsNotAdmin() throws Exception {
            given(getUserByIdQuery.execute(any(), any()))
                    .willThrow(new UnauthorizedOperationException(Role.STAFF, "obtener detalle de otro usuario"));

            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users/{id}", 5)
                            .with(staffAuthentication()))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Debe retornar 401 cuando no hay autenticación")
        void mustReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/users/{id}", 5))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.title").value("Unauthorized"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/{id} — Actualizar usuario")
    class UpdateUser {

        @Test
        @DisplayName("Debe retornar 200 con el usuario actualizado cuando el actor es ADMIN")
        void mustReturn200WithUpdatedUserWhenAdmin() throws Exception {
            var updatedUser = sampleUser(5L, "maria", Role.STAFF);
            given(updateUserUseCase.execute(any(), any())).willReturn(updatedUser);

            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/{id}", 5)
                            .with(adminAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "firstName": "Juan",
                                      "lastName": "Pérez",
                                      "identification": "ID-5",
                                      "email": "maria@uniquindio.edu.co",
                                      "role": "STAFF",
                                      "active": true
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.username").value("maria"))
                    .andExpect(jsonPath("$.role").value("STAFF"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("Debe retornar 400 cuando el cuerpo de la petición es inválido")
        void mustReturn400WhenPayloadIsInvalid() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/{id}", 5)
                            .with(adminAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "firstName": "",
                                      "lastName": "",
                                      "identification": "",
                                      "email": "invalid-email",
                                      "role": null,
                                      "active": null
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }

        @Test
        @DisplayName("Debe retornar 400 cuando faltan campos obligatorios")
        void mustReturn400WhenRequiredFieldsMissing() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/{id}", 5)
                            .with(adminAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "firstName": "Juan"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("Debe retornar 403 cuando el actor no tiene rol ADMIN")
        void mustReturn403WhenActorIsNotAdmin() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/{id}", 5)
                            .with(staffAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "firstName": "Juan",
                                      "lastName": "Pérez",
                                      "identification": "ID-5",
                                      "email": "maria@uniquindio.edu.co",
                                      "role": "STAFF",
                                      "active": true
                                    }
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("Debe retornar 401 cuando no hay autenticación")
        void mustReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/{id}", 5)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "firstName": "Juan",
                                      "lastName": "Pérez",
                                      "identification": "ID-5",
                                      "email": "maria@uniquindio.edu.co",
                                      "role": "STAFF",
                                      "active": true
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.title").value("Unauthorized"));
        }

        @Test
        @DisplayName("Debe retornar 409 cuando el caso de uso lanza BusinessRuleViolationException (auto-mutación)")
        void mustReturn409WhenSelfMutationGuardTriggered() throws Exception {
            given(updateUserUseCase.execute(any(), any()))
                    .willThrow(new BusinessRuleViolationException("Un administrador no puede modificar su propio usuario"));

            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/{id}", 99)
                            .with(adminAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "firstName": "Admin",
                                      "lastName": "Principal",
                                      "identification": "ID-99",
                                      "email": "admin@uniquindio.edu.co",
                                      "role": "ADMIN",
                                      "active": true
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.title").value("Conflict"));
        }

        @Test
        @DisplayName("Debe retornar 404 cuando el caso de uso no encuentra el usuario a actualizar")
        void mustReturn404WhenUserToUpdateDoesNotExist() throws Exception {
            given(updateUserUseCase.execute(any(), any()))
                    .willThrow(new EntityNotFoundException("User", "id", 999L));

            mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/users/{id}", 999)
                            .with(adminAuthentication())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "firstName": "Juan",
                                      "lastName": "Pérez",
                                      "identification": "ID-999",
                                      "email": "juan@uniquindio.edu.co",
                                      "role": "STUDENT",
                                      "active": true
                                    }
                                    """))
                    .andExpect(status().isNotFound())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value("Not Found"));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static User sampleUser(long id, String username, Role role) {
        return User.reconstitute(
                new UserId(id),
                new Username(username),
                "Juan",
                "Pérez",
                new PasswordHash("encoded-password"),
                new Identification("ID-" + id),
                new Email(username + "@uniquindio.edu.co"),
                role,
                true
        );
    }

    private RequestPostProcessor adminAuthentication() {
        return authentication(99L, "admin", Role.ADMIN);
    }

    private RequestPostProcessor staffAuthentication() {
        return authentication(10L, "staff01", Role.STAFF);
    }

    private RequestPostProcessor studentAuthentication() {
        return authentication(7L, "jperez", Role.STUDENT);
    }

    private RequestPostProcessor authentication(long id, String username, Role role) {
        var principal = new AuthenticatedUser(id, username, role, true);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    @TestConfiguration
    static class TestMappersConfiguration {
        @Bean
        UserRestMapper userRestMapper() {
            return new UserRestMapper();
        }

        @Bean
        AuthenticatedActorMapper authenticatedActorMapper() {
            return new AuthenticatedActorMapper();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
