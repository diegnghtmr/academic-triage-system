package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.exception.IdempotencyFingerprintMismatchException;
import co.edu.uniquindio.triage.application.exception.IdempotencyRequestInProgressException;
import co.edu.uniquindio.triage.application.exception.MissingIdempotencyKeyException;
import co.edu.uniquindio.triage.application.port.in.request.*;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.*;
import co.edu.uniquindio.triage.domain.model.id.*;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.RequestRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.RequestRestMapperImpl;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.HttpIdempotencySupport;
import co.edu.uniquindio.triage.infrastructure.adapter.out.security.AuthenticatedUser;
import co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RequestController.class)
@ContextConfiguration(classes = {
        RequestController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        RequestControllerIdempotencyTest.TestMappersConfiguration.class,
        RequestControllerIdempotencyTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        RequestControllerIdempotencyTest.TestMappersConfiguration.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class RequestControllerIdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateRequestUseCase createRequestUseCase;

    @MockitoBean
    private ClassifyRequestUseCase classifyRequestUseCase;

    @MockitoBean
    private PrioritizeRequestUseCase prioritizeRequestUseCase;

    @MockitoBean
    private AssignRequestUseCase assignRequestUseCase;

    @MockitoBean
    private AttendRequestUseCase attendRequestUseCase;

    @MockitoBean
    private CloseRequestUseCase closeRequestUseCase;

    @MockitoBean
    private CancelRequestUseCase cancelRequestUseCase;

    @MockitoBean
    private RejectRequestUseCase rejectRequestUseCase;

    @MockitoBean
    private ListRequestsQuery listRequestsQuery;

    @MockitoBean
    private GetRequestDetailQuery getRequestDetailQuery;

    @MockitoBean
    private GetPrioritySuggestionQuery getPrioritySuggestionQuery;

    @MockitoBean
    private LoadUserAuthPort loadUserAuthPort;

    @MockitoBean
    private AddInternalNoteUseCase addInternalNoteUseCase;

    @MockitoBean
    private HttpIdempotencySupport httpIdempotencySupport;

    @Test
    void createMustReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/requests")
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestTypeId": 3,
                                  "originChannelId": 2,
                                  "description": "Necesito un cupo adicional para la materia",
                                  "deadline": "2026-03-15"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createMustReturn409WhenRequestIsInProgress() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new IdempotencyRequestInProgressException());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/requests")
                        .with(studentAuthentication())
                        .header("Idempotency-Key", "key-abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestTypeId": 3,
                                  "originChannelId": 2,
                                  "description": "Necesito un cupo adicional para la materia",
                                  "deadline": "2026-03-15"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createMustReturn422WhenFingerprintMismatch() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new IdempotencyFingerprintMismatchException());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/requests")
                        .with(studentAuthentication())
                        .header("Idempotency-Key", "key-abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestTypeId": 3,
                                  "originChannelId": 2,
                                  "description": "Descripción diferente a la original",
                                  "deadline": "2026-03-15"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void createMustReturn201WithIdempotencyStatusFreshOnFirstRequest() throws Exception {
        given(createRequestUseCase.execute(any(), any())).willReturn(sampleSummary());
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willAnswer(inv -> {
                    java.util.function.Supplier<?> supplier = inv.getArgument(7);
                    var result = (ResponseEntity<?>) supplier.get();
                    return ResponseEntity.status(result.getStatusCode())
                            .header("Idempotency-Status", "fresh")
                            .header(HttpHeaders.LOCATION, result.getHeaders().getFirst(HttpHeaders.LOCATION))
                            .body(result.getBody());
                });

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/requests")
                        .with(studentAuthentication())
                        .header("Idempotency-Key", "key-fresh-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestTypeId": 3,
                                  "originChannelId": 2,
                                  "description": "Necesito un cupo adicional para la materia",
                                  "deadline": "2026-03-15"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Status", "fresh"))
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/requests/42"));
    }

    @Test
    void classifyMustReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/classify", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestTypeId": 4
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void prioritizeMustReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/prioritize", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"HIGH\",\"justification\":\"Urgente por vencimiento\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void prioritizeMustReturn422WhenFingerprintMismatch() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new IdempotencyFingerprintMismatchException());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/prioritize", 42)
                        .with(staffAuthentication())
                        .header("Idempotency-Key", "key-prioritize-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"LOW\",\"justification\":\"Payload diferente al original\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void prioritizeMustReturnReplayedStatusOnSameKeyAndPayload() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willAnswer(inv -> ResponseEntity.ok()
                        .header("Idempotency-Status", "replayed")
                        .body(null));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/prioritize", 42)
                        .with(staffAuthentication())
                        .header("Idempotency-Key", "key-prioritize-replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"HIGH\",\"justification\":\"Urgente por vencimiento\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Status", "replayed"));
    }

    @Test
    void assignMustReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/assign", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignedToUserId\":5,\"observations\":\"Asignado por disponibilidad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void attendMustReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/attend", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observations\":\"Atendido en oficina hoy\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void closeMustReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/close", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"closingObservation\":\"Solicitud resuelta satisfactoriamente\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void cancelMustReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/cancel", 42)
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancellationReason\":\"Ya no necesito el cupo adicional\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectMustReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/reject", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"Documentación incompleta e insuficiente\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void addInternalNoteMustReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new MissingIdempotencyKeyException());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/requests/{requestId}/history", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observations\":\"Nota interna para el historial de la solicitud\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private static RequestSummary sampleSummary() {
        var requester = sampleUser(7L, "jperez", Role.STUDENT);
        var request = AcademicRequest.reconstitute(
                new RequestId(42L),
                "Necesito un cupo adicional para la materia",
                RequestStatus.REGISTERED,
                null, null,
                LocalDate.of(2026, 3, 15),
                LocalDateTime.of(2026, 3, 10, 8, 30),
                false, null, null, null, null,
                requester.getId().orElseThrow(),
                null,
                new OriginChannelId(2L),
                new RequestTypeId(3L),
                List.of(), List.of()
        );
        return new RequestSummary(
                request,
                new RequestType(new RequestTypeId(3L), "Cupo", "Solicitud de cupo", true),
                new OriginChannel(new OriginChannelId(2L), "Correo", true),
                requester,
                Optional.empty()
        );
    }

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

    private RequestPostProcessor studentAuthentication() {
        var principal = new AuthenticatedUser(7L, "jperez", Role.STUDENT, true);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    private RequestPostProcessor staffAuthentication() {
        var principal = new AuthenticatedUser(10L, "staff01", Role.STAFF, true);
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
        RequestRestMapper requestRestMapper() {
            return new RequestRestMapperImpl();
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
