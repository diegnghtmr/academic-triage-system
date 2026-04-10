package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.auth.AuthResult;
import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.auth.LoginUseCase;
import co.edu.uniquindio.triage.application.port.in.auth.RegisterUseCase;
import co.edu.uniquindio.triage.application.port.out.security.AuthToken;
import co.edu.uniquindio.triage.application.port.out.security.TokenProviderPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.AuthenticationFailedException;
import co.edu.uniquindio.triage.domain.exception.DuplicateUserException;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.AuthRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.security.AuthenticatedUser;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = {
        AuthController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        AuthControllerTest.TestProbeController.class,
        AuthControllerTest.TestMappersConfiguration.class,
        AuthControllerTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        AuthControllerTest.TestProbeController.class,
        AuthControllerTest.TestMappersConfiguration.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProviderPort tokenProviderPort;

    @MockitoBean
    private RegisterUseCase registerUseCase;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private LoadUserAuthPort loadUserAuthPort;

    @BeforeEach
    void stubJwtRevalidationUserLookup() {
        given(loadUserAuthPort.loadById(any(UserId.class))).willAnswer(invocation -> {
            UserId id = invocation.getArgument(0);
            if (Long.valueOf(1L).equals(id.value())) {
                return Optional.of(sampleUser(Role.STAFF));
            }
            return Optional.empty();
        });
    }

    @Test
    void registerMustReturn201WithUserContract() throws Exception {
        given(registerUseCase.register(any(), eq(Optional.empty()))).willReturn(sampleUser(Role.STUDENT));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jperez",
                                  "email": "jperez@uniquindio.edu.co",
                                  "password": "MyPassword123",
                                  "firstName": "Juan",
                                  "lastName": "Pérez",
                                  "identification": "1094123456",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value("jperez"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("STUDENT"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.password").doesNotExist());
    }

    @Test
    void adminBackedRegistrationMustPreserveElevatedRole() throws Exception {
        given(registerUseCase.register(any(), eq(Optional.of(new AuthenticatedActor(new UserId(99L), "admin", Role.ADMIN)))))
                .willReturn(sampleUser(Role.STAFF));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register")
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "staff01",
                                  "email": "staff01@uniquindio.edu.co",
                                  "password": "MyPassword123",
                                  "firstName": "Staff",
                                  "lastName": "Uno",
                                  "identification": "20001",
                                  "role": "STAFF"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("STAFF"));
    }

    @Test
    void loginMustReturnBearerPayload() throws Exception {
        given(loginUseCase.login(any())).willReturn(new AuthResult(
                new AuthToken("jwt-token", "Bearer", 86400),
                sampleUser(Role.STUDENT)
        ));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jperez",
                                  "password": "MyPassword123"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.token").value("jwt-token"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.tokenType").value("Bearer"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.user.firstName").value("Juan"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.user.lastName").value("Pérez"));
    }

    @Test
    void loginMustReturn401WhenUseCaseRejectsCredentials() throws Exception {
        given(loginUseCase.login(any())).willThrow(new AuthenticationFailedException());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jperez",
                                  "password": "WrongPassword123"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401));
    }

    @Test
    void registerMustReturn409WhenDuplicateExists() throws Exception {
        given(registerUseCase.register(any(), eq(Optional.empty())))
                .willThrow(new DuplicateUserException("username", "jperez"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jperez",
                                  "email": "jperez@uniquindio.edu.co",
                                  "password": "MyPassword123",
                                  "firstName": "Juan",
                                  "lastName": "Pérez",
                                  "identification": "1094123456"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isConflict())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(409));
    }

    @Test
    void registerMustReturn500WhenUnhandledDataIntegrityViolationOccurs() throws Exception {
        given(registerUseCase.register(any(), eq(Optional.empty())))
                .willThrow(new DataIntegrityViolationException("Simulated unclassified constraint"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "jperez",
                                  "email": "jperez@uniquindio.edu.co",
                                  "password": "MyPassword123",
                                  "firstName": "Juan",
                                  "lastName": "Pérez",
                                  "identification": "1094123456"
                                }
                                """))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(500))
                .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Internal Server Error"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.detail").value(
                        "La operación no pudo completarse por una restricción de base de datos"));
    }

    @Test
    void nonPublicRoutesMustRequireAuthentication() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/private-probe"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void validBearerTokenMustRestorePrincipalAndRoleContext() throws Exception {
        var token = tokenProviderPort.issue(sampleUser(Role.STAFF)).token();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/private-probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.username").value("jperez"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.role").value("STAFF"));
    }

    private User sampleUser(Role role) {
        return User.reconstitute(
                new UserId(1L),
                new Username("jperez"),
                "Juan",
                "Pérez",
                new PasswordHash("hash-value"),
                new Identification("1094123456"),
                new Email("jperez@uniquindio.edu.co"),
                role,
                true
        );
    }

    private RequestPostProcessor adminAuthentication() {
        var principal = new AuthenticatedUser(99L, "admin", Role.ADMIN, true);
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
        AuthRestMapper authRestMapper(UserRestMapper userRestMapper) {
            return new AuthRestMapper(userRestMapper);
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

    @RestController
    static class TestProbeController {

        @GetMapping("/api/v1/private-probe")
        Map<String, Object> privateProbe(Authentication authentication) {
            var principal = (AuthenticatedUser) authentication.getPrincipal();
            return Map.of(
                    "username", principal.username(),
                    "role", principal.role().name()
            );
        }
    }
}
