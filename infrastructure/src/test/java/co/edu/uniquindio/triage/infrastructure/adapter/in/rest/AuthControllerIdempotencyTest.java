package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.exception.IdempotencyFingerprintMismatchException;
import co.edu.uniquindio.triage.application.exception.MissingIdempotencyKeyException;
import co.edu.uniquindio.triage.application.port.in.auth.LoginUseCase;
import co.edu.uniquindio.triage.application.port.in.auth.RegisterUseCase;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.AuthRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.HttpIdempotencySupport;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ContextConfiguration(classes = {
        AuthController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        NoopLoadUserAuthPortTestConfiguration.class,
        AuthControllerIdempotencyTest.TestMappersConfiguration.class,
        AuthControllerIdempotencyTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        NoopLoadUserAuthPortTestConfiguration.class,
        AuthControllerIdempotencyTest.TestMappersConfiguration.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class AuthControllerIdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private RegisterUseCase registerUseCase;
    @MockitoBean private LoginUseCase loginUseCase;
    @MockitoBean private HttpIdempotencySupport httpIdempotencySupport;

    private static final String REGISTER_BODY = """
            {
              "username": "jperez",
              "password": "secret1234",
              "firstName": "Juan",
              "lastName": "Pérez",
              "identification": "1234567890",
              "email": "jperez@uniquindio.edu.co",
              "role": "STUDENT"
            }
            """;

    @Test
    void registerMustReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registerMustReturn422WhenFingerprintMismatch() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new IdempotencyFingerprintMismatchException());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register")
                        .header("Idempotency-Key", "key-register-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
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
}
