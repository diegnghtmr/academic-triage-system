package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.user.GetUserByIdQuery;
import co.edu.uniquindio.triage.application.port.in.user.GetUserVersionUseCase;
import co.edu.uniquindio.triage.application.port.in.user.GetUsersQuery;
import co.edu.uniquindio.triage.application.port.in.user.UpdateUserUseCase;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.ETagSupport;
import co.edu.uniquindio.triage.infrastructure.adapter.out.security.AuthenticatedUser;
import co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration;
import co.edu.uniquindio.triage.infrastructure.testsupport.NoopLoadUserAuthPortTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ContextConfiguration(classes = {
        UserController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        NoopLoadUserAuthPortTestConfiguration.class,
        UserControllerETagTest.TestMappersConfiguration.class,
        UserControllerETagTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        NoopLoadUserAuthPortTestConfiguration.class,
        UserControllerETagTest.TestMappersConfiguration.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class UserControllerETagTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetUsersQuery getUsersQuery;

    @MockitoBean
    private GetUserByIdQuery getUserByIdQuery;

    @MockitoBean
    private UpdateUserUseCase updateUserUseCase;

    @MockitoBean
    private GetUserVersionUseCase getUserVersionUseCase;

    @Test
    void getByIdMustEmitETagHeader() throws Exception {
        given(getUserByIdQuery.execute(any(), any())).willReturn(Optional.of(sampleUser(1L)));
        given(getUserVersionUseCase.getVersionById(any())).willReturn(Optional.of(3L));

        mockMvc.perform(get("/api/v1/users/{id}", 1).with(admin()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"3\""));
    }

    @Test
    void getByIdMustReturn200WithoutETagWhenVersionAbsent() throws Exception {
        given(getUserByIdQuery.execute(any(), any())).willReturn(Optional.of(sampleUser(1L)));
        given(getUserVersionUseCase.getVersionById(any())).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/{id}", 1).with(admin()))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("ETag"));
    }

    @Test
    void putMustReturn428WhenIfMatchMissing() throws Exception {
        given(getUserVersionUseCase.getVersionById(any())).willReturn(Optional.of(2L));

        mockMvc.perform(put("/api/v1/users/{id}", 1)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateBody()))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.status").value(428));
    }

    @Test
    void putMustReturn412WhenIfMatchVersionStalale() throws Exception {
        given(getUserVersionUseCase.getVersionById(any())).willReturn(Optional.of(5L));

        mockMvc.perform(put("/api/v1/users/{id}", 1)
                        .with(admin())
                        .header("If-Match", "\"2\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateBody()))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.status").value(412));
    }

    @Test
    void putMustReturn200WhenIfMatchCorrect() throws Exception {
        given(getUserVersionUseCase.getVersionById(any())).willReturn(Optional.of(2L));
        given(updateUserUseCase.execute(any(), any())).willReturn(sampleUser(1L));

        mockMvc.perform(put("/api/v1/users/{id}", 1)
                        .with(admin())
                        .header("If-Match", "\"2\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    private User sampleUser(Long id) {
        return User.reconstitute(
                new UserId(id),
                new Username("admin"),
                "Admin",
                "User",
                new PasswordHash("hash"),
                new Identification("12345678"),
                new Email("admin@test.com"),
                Role.ADMIN,
                true
        );
    }

    private String validUpdateBody() {
        return """
                {
                  "firstName": "Updated",
                  "lastName": "User",
                  "identification": "12345678",
                  "email": "admin@test.com",
                  "role": "ADMIN",
                  "active": true
                }
                """;
    }

    private RequestPostProcessor admin() {
        var principal = new AuthenticatedUser(1L, "admin", Role.ADMIN, true);
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

        @Bean
        ETagSupport eTagSupport() {
            return new ETagSupport();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    static class TestApplication {
    }
}
