package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice;

import co.edu.uniquindio.triage.application.exception.ETagMismatchException;
import co.edu.uniquindio.triage.application.exception.IdempotencyFingerprintMismatchException;
import co.edu.uniquindio.triage.application.exception.IdempotencyRequestInProgressException;
import co.edu.uniquindio.triage.application.exception.MissingIdempotencyKeyException;
import co.edu.uniquindio.triage.application.exception.MissingIfMatchPreconditionException;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = {
        GlobalExceptionHandlerIdempotencyTest.ProbeController.class,
        GlobalExceptionHandlerIdempotencyTest.TestApplication.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class
})
@Import({GlobalExceptionHandler.class, SecurityConfiguration.class})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class GlobalExceptionHandlerIdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoadUserAuthPort loadUserAuthPort;

    @Test
    @WithMockUser
    void missingIdempotencyKeyShouldReturn400() throws Exception {
        mockMvc.perform(get("/probe/missing-key").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("urn:problem:idempotency:key-required"));
    }

    @Test
    @WithMockUser
    void idempotencyInProgressShouldReturn409() throws Exception {
        mockMvc.perform(get("/probe/in-progress").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("urn:problem:idempotency:request-in-progress"));
    }

    @Test
    @WithMockUser
    void fingerprintMismatchShouldReturn422() throws Exception {
        mockMvc.perform(get("/probe/fingerprint-mismatch").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("urn:problem:idempotency:fingerprint-mismatch"));
    }

    @Test
    @WithMockUser
    void missingIfMatchShouldReturn428() throws Exception {
        mockMvc.perform(get("/probe/missing-if-match").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(428))
                .andExpect(jsonPath("$.status").value(428))
                .andExpect(jsonPath("$.type").value("urn:problem:precondition:if-match-required"));
    }

    @Test
    @WithMockUser
    void etagMismatchShouldReturn412() throws Exception {
        mockMvc.perform(get("/probe/etag-mismatch").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.status").value(412))
                .andExpect(jsonPath("$.type").value("urn:problem:precondition:etag-mismatch"));
    }

    @RestController
    @RequestMapping("/probe")
    static class ProbeController {
        @GetMapping("/missing-key")
        void missingKey() { throw new MissingIdempotencyKeyException(); }

        @GetMapping("/in-progress")
        void inProgress() { throw new IdempotencyRequestInProgressException(); }

        @GetMapping("/fingerprint-mismatch")
        void fingerprintMismatch() { throw new IdempotencyFingerprintMismatchException(); }

        @GetMapping("/missing-if-match")
        void missingIfMatch() { throw new MissingIfMatchPreconditionException(); }

        @GetMapping("/etag-mismatch")
        void etagMismatch() { throw new ETagMismatchException(); }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
