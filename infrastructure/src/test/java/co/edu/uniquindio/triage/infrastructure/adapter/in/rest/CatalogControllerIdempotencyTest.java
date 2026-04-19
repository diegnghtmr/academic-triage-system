package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.exception.IdempotencyFingerprintMismatchException;
import co.edu.uniquindio.triage.application.exception.MissingIdempotencyKeyException;
import co.edu.uniquindio.triage.application.port.in.catalog.*;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.CatalogRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.ETagSupport;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.HttpIdempotencySupport;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogController.class)
@ContextConfiguration(classes = {
        CatalogController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        NoopLoadUserAuthPortTestConfiguration.class,
        CatalogControllerIdempotencyTest.TestMappersConfiguration.class,
        CatalogControllerIdempotencyTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        NoopLoadUserAuthPortTestConfiguration.class,
        CatalogControllerIdempotencyTest.TestMappersConfiguration.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class CatalogControllerIdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ListRequestTypesQuery listRequestTypesQuery;
    @MockitoBean private GetRequestTypeQuery getRequestTypeQuery;
    @MockitoBean private CreateRequestTypeUseCase createRequestTypeUseCase;
    @MockitoBean private UpdateRequestTypeUseCase updateRequestTypeUseCase;
    @MockitoBean private ListOriginChannelsQuery listOriginChannelsQuery;
    @MockitoBean private GetOriginChannelQuery getOriginChannelQuery;
    @MockitoBean private CreateOriginChannelUseCase createOriginChannelUseCase;
    @MockitoBean private UpdateOriginChannelUseCase updateOriginChannelUseCase;
    @MockitoBean private GetRequestTypeVersionUseCase getRequestTypeVersionUseCase;
    @MockitoBean private GetOriginChannelVersionUseCase getOriginChannelVersionUseCase;
    @MockitoBean private HttpIdempotencySupport httpIdempotencySupport;

    // ────────── Request Types ──────────

    @Test
    void createRequestTypeMustReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/catalogs/request-types")
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cupo adicional\",\"description\":\"Solicitud de cupo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createRequestTypeMustReturn422WhenFingerprintMismatch() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new IdempotencyFingerprintMismatchException());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/catalogs/request-types")
                        .with(adminAuthentication())
                        .header("Idempotency-Key", "key-rt-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cupo diferente\",\"description\":\"Cuerpo distinto al original\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void createRequestTypeMustReturnReplayedOnSameKeyAndPayload() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(ResponseEntity.created(java.net.URI.create("/api/v1/catalogs/request-types/5"))
                        .header("Idempotency-Status", "replayed")
                        .build());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/catalogs/request-types")
                        .with(adminAuthentication())
                        .header("Idempotency-Key", "key-rt-replay-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cupo adicional\",\"description\":\"Solicitud de cupo\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Status", "replayed"));
    }

    // ────────── Origin Channels ──────────

    @Test
    void createOriginChannelMustReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/catalogs/origin-channels")
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Presencial\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createOriginChannelMustReturn422WhenFingerprintMismatch() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new IdempotencyFingerprintMismatchException());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/catalogs/origin-channels")
                        .with(adminAuthentication())
                        .header("Idempotency-Key", "key-oc-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Virtual — diferente\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    private RequestPostProcessor adminAuthentication() {
        var principal = new AuthenticatedUser(1L, "admin01", Role.ADMIN, true);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    @TestConfiguration
    static class TestMappersConfiguration {
        @Bean
        CatalogRestMapper catalogRestMapper() {
            return new CatalogRestMapper();
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
    static class TestApplication {
    }
}
