package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.request.AddInternalNoteUseCase;
import co.edu.uniquindio.triage.application.port.in.request.AssignRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.AttendRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.CancelRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.ClassifyRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.CloseRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.CreateRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.GetPrioritySuggestionQuery;
import co.edu.uniquindio.triage.application.port.in.request.GetRequestDetailQuery;
import co.edu.uniquindio.triage.application.port.in.request.MatchedBusinessRuleSummary;
import co.edu.uniquindio.triage.application.port.in.request.PrioritySuggestionResult;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.in.common.Page;
import co.edu.uniquindio.triage.application.port.in.request.ListRequestsQuery;
import co.edu.uniquindio.triage.application.port.in.request.PrioritizeRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.RejectRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.in.request.RequestHistoryDetail;
import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;
import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.exception.InvalidStateTransitionException;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.RequestHistory;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.RequestHistoryId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.RequestRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.RequestRestMapperImpl;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.HttpIdempotencySupport;
import co.edu.uniquindio.triage.infrastructure.adapter.out.security.AuthenticatedUser;
import co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestController.class)
@ContextConfiguration(classes = {
        RequestController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        RequestControllerTest.TestMappersConfiguration.class,
        RequestControllerTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        RequestControllerTest.TestMappersConfiguration.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class RequestControllerTest {

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

    @BeforeEach
    void configureIdempotencyPassThrough() {
        willAnswer(inv -> ((java.util.function.Supplier<?>) inv.getArgument(7)).get())
                .given(httpIdempotencySupport).execute(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createMustReturn201WithLocationAndResponseBody() throws Exception {
        given(createRequestUseCase.execute(any(), any())).willReturn(sampleSummary());

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
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/requests/42"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("REGISTERED"))
                .andExpect(jsonPath("$.requestType.id").value(3))
                .andExpect(jsonPath("$.requester.username").value("jperez"));
    }

    @Test
    void createMustReturn400WhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/requests")
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestTypeId": 3,
                                  "originChannelId": 2,
                                  "description": "corta"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("La solicitud contiene errores de validación"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("description"));
    }

    @Test
    void createMustReturn403WhenUseCaseRejectsActor() throws Exception {
        given(createRequestUseCase.execute(any(), any())).willThrow(new UnauthorizedOperationException(Role.ADMIN, "create request"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/requests")
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestTypeId": 3,
                                  "originChannelId": 2,
                                  "description": "Necesito un cupo adicional para la materia"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void classifyMustReturn200AndBindCommand() throws Exception {
        given(classifyRequestUseCase.execute(any(), any())).willReturn(classifiedSummary());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/classify", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestTypeId": 4,
                                  "observations": "  Reclasificada por soporte académico.  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("CLASSIFIED"))
                .andExpect(jsonPath("$.requestType.id").value(4));

        verify(classifyRequestUseCase).execute(
                argThat(command -> command.requestId().equals(new RequestId(42L))
                        && command.requestTypeId().equals(new RequestTypeId(4L))
                        && "Reclasificada por soporte académico.".equals(command.observations())),
                argThat(actor -> actor.role() == Role.STAFF && actor.userId().equals(new UserId(10L)))
        );
    }

    @Test
    void classifyMustReturn400WhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/classify", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observations": "Sin tipo"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("requestTypeId"));
    }

    @Test
    void classifyMustReturn403WhenUseCaseRejectsActor() throws Exception {
        given(classifyRequestUseCase.execute(any(), any())).willThrow(new UnauthorizedOperationException(Role.STUDENT, "classify request"));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/classify", 42)
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestTypeId": 4
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void classifyMustReturn409WhenLifecycleTransitionIsInvalid() throws Exception {
        given(classifyRequestUseCase.execute(any(), any())).willThrow(new InvalidStateTransitionException(RequestStatus.CANCELLED, RequestStatus.CLASSIFIED));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/classify", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestTypeId": 4
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void classifyMustReturn404WhenRequestDoesNotExist() throws Exception {
        given(classifyRequestUseCase.execute(any(), any())).willThrow(new RequestNotFoundException(new RequestId(42L)));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/classify", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestTypeId": 4
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void prioritizeMustReturn200AndBindCommand() throws Exception {
        given(prioritizeRequestUseCase.execute(any(), any())).willReturn(prioritizedSummary());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/prioritize", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "priority": "HIGH",
                                  "justification": "  Impacta matrícula en menos de 72 horas.  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("CLASSIFIED"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.priorityJustification").value("Impacta matrícula en menos de 72 horas."));

        verify(prioritizeRequestUseCase).execute(
                argThat(command -> command.requestId().equals(new RequestId(42L))
                        && command.priority() == Priority.HIGH
                        && "Impacta matrícula en menos de 72 horas.".equals(command.justification())),
                argThat(actor -> actor.role() == Role.STAFF && actor.userId().equals(new UserId(10L)))
        );
    }

    @Test
    void prioritizeMustReturn400WhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/prioritize", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "priority": "HIGH",
                                  "justification": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("justification"));
    }

    @Test
    void prioritizeMustReturn404WhenRequestDoesNotExist() throws Exception {
        given(prioritizeRequestUseCase.execute(any(), any())).willThrow(new RequestNotFoundException(new RequestId(42L)));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/prioritize", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "priority": "HIGH",
                                  "justification": "Impacta matrícula en menos de 72 horas."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void prioritizeMustReturn409WhenRequestIsNotClassified() throws Exception {
        given(prioritizeRequestUseCase.execute(any(), any())).willThrow(new InvalidStateTransitionException(RequestStatus.REGISTERED, RequestStatus.CLASSIFIED));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/prioritize", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "priority": "HIGH",
                                  "justification": "Impacta matrícula en menos de 72 horas."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void prioritizeMustReturn403WhenUseCaseRejectsActor() throws Exception {
        given(prioritizeRequestUseCase.execute(any(), any())).willThrow(new UnauthorizedOperationException(Role.STUDENT, "prioritize request"));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/prioritize", 42)
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "priority": "HIGH",
                                  "justification": "Impacta matrícula en menos de 72 horas."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void assignMustReturn200AndBindCommand() throws Exception {
        given(assignRequestUseCase.execute(any(), any())).willReturn(inProgressAssignedSummary());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/assign", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedToUserId": 15,
                                  "observations": "  Se asigna a coordinación académica.  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.assignedTo.id").value(15));

        verify(assignRequestUseCase).execute(
                argThat(command -> command.requestId().equals(new RequestId(42L))
                        && command.assignedToUserId().equals(new UserId(15L))
                        && "Se asigna a coordinación académica.".equals(command.observations())),
                argThat(actor -> actor.role() == Role.STAFF && actor.userId().equals(new UserId(10L)))
        );
    }

    @Test
    void assignMustReturn400WhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/assign", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedToUserId": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("assignedToUserId"));
    }

    @Test
    void assignMustReturn409WhenRequestIsNotPrioritizedYet() throws Exception {
        given(assignRequestUseCase.execute(any(), any())).willThrow(new IllegalStateException("La solicitud debe estar priorizada antes de asignarse"));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/assign", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedToUserId": 15
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("La solicitud debe estar priorizada antes de asignarse"));
    }

    @Test
    void assignMustReturn404WhenRequestDoesNotExist() throws Exception {
        given(assignRequestUseCase.execute(any(), any())).willThrow(new RequestNotFoundException(new RequestId(42L)));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/assign", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedToUserId": 15
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void assignMustReturn404WhenAssigneeDoesNotExist() throws Exception {
        given(assignRequestUseCase.execute(any(), any())).willThrow(new EntityNotFoundException("User", "id", 15L));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/assign", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedToUserId": 15
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void assignMustReturn403WhenUseCaseRejectsActor() throws Exception {
        given(assignRequestUseCase.execute(any(), any())).willThrow(new UnauthorizedOperationException(Role.STUDENT, "assign request"));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/assign", 42)
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assignedToUserId": 15
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void attendMustReturn200AndBindCommand() throws Exception {
        given(attendRequestUseCase.execute(any(), any())).willReturn(attendedSummary());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/attend", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observations": "  Se gestionó el cupo con la coordinación y quedó resuelto.  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("ATTENDED"));

        verify(attendRequestUseCase).execute(
                argThat(command -> command.requestId().equals(new RequestId(42L))
                        && "Se gestionó el cupo con la coordinación y quedó resuelto.".equals(command.observations())),
                argThat(actor -> actor.role() == Role.STAFF && actor.userId().equals(new UserId(10L)))
        );
    }

    @Test
    void attendMustReturn400WhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/attend", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observations": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("observations"));
    }

    @Test
    void attendMustReturn403WhenUseCaseRejectsActor() throws Exception {
        given(attendRequestUseCase.execute(any(), any())).willThrow(new UnauthorizedOperationException(Role.STUDENT, "attend request"));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/attend", 42)
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observations": "Se gestionó el cupo con la coordinación."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void attendMustReturn404WhenRequestDoesNotExist() throws Exception {
        given(attendRequestUseCase.execute(any(), any())).willThrow(new RequestNotFoundException(new RequestId(42L)));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/attend", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observations": "Se gestionó el cupo con la coordinación."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void attendMustReturn409WhenLifecycleTransitionIsInvalid() throws Exception {
        given(attendRequestUseCase.execute(any(), any())).willThrow(new InvalidStateTransitionException(RequestStatus.CLASSIFIED, RequestStatus.ATTENDED));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/attend", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observations": "Se gestionó el cupo con la coordinación."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void closeMustReturn200AndBindCommand() throws Exception {
        given(closeRequestUseCase.execute(any(), any())).willReturn(closedSummary());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/close", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "closingObservation": "  La solicitud quedó resuelta y validada con el estudiante.  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closingObservation").value("La solicitud quedó resuelta y validada con el estudiante."));

        verify(closeRequestUseCase).execute(
                argThat(command -> command.requestId().equals(new RequestId(42L))
                        && "La solicitud quedó resuelta y validada con el estudiante.".equals(command.closingObservation())),
                argThat(actor -> actor.role() == Role.STAFF && actor.userId().equals(new UserId(10L)))
        );
    }

    @Test
    void closeMustReturn400WhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/close", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "closingObservation": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("closingObservation"));
    }

    @Test
    void closeMustReturn403WhenUseCaseRejectsActor() throws Exception {
        given(closeRequestUseCase.execute(any(), any())).willThrow(new UnauthorizedOperationException(Role.STUDENT, "close request"));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/close", 42)
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "closingObservation": "La solicitud quedó resuelta y validada con el estudiante."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void closeMustReturn404WhenRequestDoesNotExist() throws Exception {
        given(closeRequestUseCase.execute(any(), any())).willThrow(new RequestNotFoundException(new RequestId(42L)));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/close", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "closingObservation": "La solicitud quedó resuelta y validada con el estudiante."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void closeMustReturn409WhenLifecycleTransitionIsInvalid() throws Exception {
        given(closeRequestUseCase.execute(any(), any())).willThrow(new InvalidStateTransitionException(RequestStatus.IN_PROGRESS, RequestStatus.CLOSED));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/close", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "closingObservation": "La solicitud quedó resuelta y validada con el estudiante."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void cancelMustReturn200AndBindCommand() throws Exception {
        given(cancelRequestUseCase.execute(any(), any())).willReturn(cancelledSummary());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/cancel", 42)
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cancellationReason": "  Ya no necesito el trámite porque resolví el problema.  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Ya no necesito el trámite porque resolví el problema."));

        verify(cancelRequestUseCase).execute(
                argThat(command -> command.requestId().equals(new RequestId(42L))
                        && "Ya no necesito el trámite porque resolví el problema.".equals(command.cancellationReason())),
                argThat(actor -> actor.role() == Role.STUDENT && actor.userId().equals(new UserId(7L)))
        );
    }

    @Test
    void cancelMustReturn400WhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/cancel", 42)
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cancellationReason": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("cancellationReason"));
    }

    @Test
    void cancelMustReturn403WhenUseCaseRejectsActor() throws Exception {
        given(cancelRequestUseCase.execute(any(), any())).willThrow(new UnauthorizedOperationException(Role.STUDENT, "cancel request"));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/cancel", 42)
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cancellationReason": "Ya no necesito el trámite porque resolví el problema."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void cancelMustReturn404WhenRequestDoesNotExist() throws Exception {
        given(cancelRequestUseCase.execute(any(), any())).willThrow(new RequestNotFoundException(new RequestId(42L)));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/cancel", 42)
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cancellationReason": "Ya no necesito el trámite porque resolví el problema."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void cancelMustReturn409WhenLifecycleTransitionIsInvalid() throws Exception {
        given(cancelRequestUseCase.execute(any(), any())).willThrow(new InvalidStateTransitionException(RequestStatus.IN_PROGRESS, RequestStatus.CANCELLED));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/cancel", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cancellationReason": "Se cancela porque el estudiante resolvió el caso por otra vía."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void rejectMustReturn200AndBindCommand() throws Exception {
        given(rejectRequestUseCase.execute(any(), any())).willReturn(rejectedSummary());

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/reject", 42)
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rejectionReason": "  La solicitud no cumple los requisitos documentales.  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("La solicitud no cumple los requisitos documentales."));

        verify(rejectRequestUseCase).execute(
                argThat(command -> command.requestId().equals(new RequestId(42L))
                        && "La solicitud no cumple los requisitos documentales.".equals(command.rejectionReason())),
                argThat(actor -> actor.role() == Role.ADMIN && actor.userId().equals(new UserId(99L)))
        );
    }

    @Test
    void rejectMustReturn400WhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/reject", 42)
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rejectionReason": "abc"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("rejectionReason"));
    }

    @Test
    void rejectMustReturn403WhenUseCaseRejectsActor() throws Exception {
        given(rejectRequestUseCase.execute(any(), any())).willThrow(new UnauthorizedOperationException(Role.STAFF, "reject request"));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/reject", 42)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rejectionReason": "La solicitud no cumple los requisitos documentales."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void rejectMustReturn404WhenRequestDoesNotExist() throws Exception {
        given(rejectRequestUseCase.execute(any(), any())).willThrow(new RequestNotFoundException(new RequestId(42L)));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/reject", 42)
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rejectionReason": "La solicitud no cumple los requisitos documentales."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void rejectMustReturn409WhenLifecycleTransitionIsInvalid() throws Exception {
        given(rejectRequestUseCase.execute(any(), any())).willThrow(new InvalidStateTransitionException(RequestStatus.CLASSIFIED, RequestStatus.REJECTED));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/requests/{requestId}/reject", 42)
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rejectionReason": "La solicitud no cumple los requisitos documentales."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void listMustBindFiltersAndReturnPagedContract() throws Exception {
        given(listRequestsQuery.execute(any(), any())).willReturn(new Page<>(List.of(sampleSummary()), 1, 1, 0, 20));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests")
                        .with(staffAuthentication())
                        .param("status", "REGISTERED")
                        .param("requestTypeId", "3")
                        .param("priority", "HIGH")
                        .param("assignedToUserId", "11")
                        .param("requesterUserId", "7")
                        .param("dateFrom", "2026-03-01")
                        .param("dateTo", "2026-03-31")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "registrationDateTime,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(20));

        verify(listRequestsQuery).execute(
                argThat(query -> query.status().filter(RequestStatus.REGISTERED::equals).isPresent()
                        && query.requestTypeId().map(RequestTypeId::value).filter(value -> value == 3L).isPresent()
                        && query.priority().filter(priority -> priority.name().equals("HIGH")).isPresent()
                        && query.assignedToUserId().map(UserId::value).filter(value -> value == 11L).isPresent()
                        && query.requesterUserId().map(UserId::value).filter(value -> value == 7L).isPresent()
                        && query.dateFrom().filter(LocalDate.of(2026, 3, 1)::equals).isPresent()
                        && query.dateTo().filter(LocalDate.of(2026, 3, 31)::equals).isPresent()
                        && query.sort().equals("registrationDateTime,desc")),
                argThat(actor -> actor.role() == Role.STAFF && actor.userId().equals(new UserId(10L)))
        );
    }

    @Test
    void listMustReturn400WhenEnumFilterIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests")
                        .with(staffAuthentication())
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("El parámetro 'status' tiene un valor inválido"));
    }

    @Test
    void listMustReturn400WhenPageIsNegative() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests")
                        .with(staffAuthentication())
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("La página no puede ser negativa"));
    }

    @Test
    void listMustReturn400WhenSizeIsOutOfRange() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests")
                        .with(staffAuthentication())
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("El tamaño de página debe estar entre 1 y 100"));
    }

    @Test
    void listMustReturn400WhenDateRangeIsInverted() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests")
                        .with(staffAuthentication())
                        .param("dateFrom", "2026-03-31")
                        .param("dateTo", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("dateFrom no puede ser posterior a dateTo"));
    }

    @Test
    void listMustReturn400WhenSortDoesNotIncludeDirection() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests")
                        .with(staffAuthentication())
                        .param("sort", "registrationDateTime"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("El sort debe incluir campo y dirección separados por coma"));
    }

    @Test
    void detailMustReturnRequestWithHistory() throws Exception {
        given(getRequestDetailQuery.execute(any(), any())).willReturn(sampleDetail());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests/{requestId}", 42)
                        .with(studentAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.history[0].action").value("REGISTERED"))
                .andExpect(jsonPath("$.history[0].performedBy.username").value("jperez"));
    }

    @Test
    void detailMustExposeAttendedHistoryObservation() throws Exception {
        given(getRequestDetailQuery.execute(any(), any())).willReturn(sampleDetailWithAttendanceHistory());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests/{requestId}", 42)
                        .with(staffAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATTENDED"))
                .andExpect(jsonPath("$.history[1].action").value("ATTENDED"))
                .andExpect(jsonPath("$.history[1].observations").value("Se gestionó el cupo con la coordinación y se notificó al estudiante."))
                .andExpect(jsonPath("$.history[1].performedBy.username").value("staff15"));
    }

    @Test
    void detailMustReturn403WhenActorCannotAccessRequest() throws Exception {
        given(getRequestDetailQuery.execute(any(), any())).willThrow(new UnauthorizedOperationException(Role.STUDENT, "get request detail"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests/{requestId}", 42)
                        .with(studentAuthentication()))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void detailMustReturn404WhenRequestDoesNotExist() throws Exception {
        given(getRequestDetailQuery.execute(any(), any())).willThrow(new RequestNotFoundException(new RequestId(42L)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests/{requestId}", 42)
                        .with(staffAuthentication()))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void prioritySuggestionMustReturn200WithMatches() throws Exception {
        given(getPrioritySuggestionQuery.execute(any(), any())).willReturn(
                new PrioritySuggestionResult(Priority.HIGH, List.of(
                        new MatchedBusinessRuleSummary(new BusinessRuleId(1L), "Regla cupos", Priority.HIGH))));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests/{requestId}/priority-suggestion", 42)
                        .with(staffAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedPriority").value("HIGH"))
                .andExpect(jsonPath("$.matchedRules[0].ruleId").value(1))
                .andExpect(jsonPath("$.matchedRules[0].name").value("Regla cupos"))
                .andExpect(jsonPath("$.matchedRules[0].resultingPriority").value("HIGH"));
    }

    @Test
    void prioritySuggestionMustReturn403WhenStudentCannotAccess() throws Exception {
        given(getPrioritySuggestionQuery.execute(any(), any()))
                .willThrow(new UnauthorizedOperationException(Role.STUDENT, "sugerencia de prioridad"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests/{requestId}/priority-suggestion", 42)
                        .with(studentAuthentication()))
                .andExpect(status().isForbidden());
    }

    @Test
    void detailMustReturn400WhenPathVariableCannotBeParsed() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests/{requestId}", "abc")
                        .with(staffAuthentication()))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("El parámetro 'requestId' tiene un valor inválido"));
    }

    @Test
    void requestRoutesMustRequireAuthentication() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    private static RequestSummary sampleSummary() {
        return sampleSummary(new RequestTypeId(3L), RequestStatus.REGISTERED, null, null, Optional.empty());
    }

    private static RequestSummary classifiedSummary() {
        return sampleSummary(new RequestTypeId(4L), RequestStatus.CLASSIFIED, null, null, Optional.empty());
    }

    private static RequestSummary prioritizedSummary() {
        return sampleSummary(new RequestTypeId(4L), RequestStatus.CLASSIFIED, Priority.HIGH,
                "Impacta matrícula en menos de 72 horas.", Optional.empty());
    }

    private static RequestSummary inProgressAssignedSummary() {
        var assignee = sampleUser(15L, "staff15", Role.STAFF);
        return sampleSummary(new RequestTypeId(4L), RequestStatus.IN_PROGRESS, Priority.HIGH,
                "Impacta matrícula en menos de 72 horas.", Optional.of(assignee));
    }

    private static RequestSummary attendedSummary() {
        var assignee = sampleUser(15L, "staff15", Role.STAFF);
        return sampleSummary(new RequestTypeId(4L), RequestStatus.ATTENDED, Priority.HIGH,
                "Impacta matrícula en menos de 72 horas.", Optional.of(assignee));
    }

    private static RequestSummary closedSummary() {
        var assignee = sampleUser(15L, "staff15", Role.STAFF);
        return sampleSummary(new RequestTypeId(4L), RequestStatus.CLOSED, Priority.HIGH,
                "Impacta matrícula en menos de 72 horas.", "La solicitud quedó resuelta y validada con el estudiante.",
                Optional.of(assignee));
    }

    private static RequestSummary cancelledSummary() {
        return sampleSummary(new RequestTypeId(4L), RequestStatus.CANCELLED, Priority.HIGH,
                "Impacta matrícula en menos de 72 horas.", null,
                "Ya no necesito el trámite porque resolví el problema.", null,
                Optional.empty());
    }

    private static RequestSummary rejectedSummary() {
        return sampleSummary(new RequestTypeId(4L), RequestStatus.REJECTED, Priority.HIGH,
                "Impacta matrícula en menos de 72 horas.", null,
                null, "La solicitud no cumple los requisitos documentales.",
                Optional.empty());
    }

    private static RequestSummary sampleSummary(RequestTypeId requestTypeId,
                                                RequestStatus status,
                                                Priority priority,
                                                String priorityJustification,
                                                Optional<User> assignedTo) {
        return sampleSummary(requestTypeId, status, priority, priorityJustification, null, null, null, assignedTo);
    }

    private static RequestSummary sampleSummary(RequestTypeId requestTypeId,
                                                RequestStatus status,
                                                Priority priority,
                                                String priorityJustification,
                                                String closingObservation,
                                                Optional<User> assignedTo) {
        return sampleSummary(requestTypeId, status, priority, priorityJustification, closingObservation, null, null, assignedTo);
    }

    private static RequestSummary sampleSummary(RequestTypeId requestTypeId,
                                                RequestStatus status,
                                                Priority priority,
                                                String priorityJustification,
                                                String closingObservation,
                                                String cancellationReason,
                                                String rejectionReason,
                                                Optional<User> assignedTo) {
        var requester = sampleUser(7L, "jperez", Role.STUDENT);
        var request = AcademicRequest.reconstitute(
                new RequestId(42L),
                "Necesito un cupo adicional para la materia",
                status,
                priority,
                priorityJustification,
                LocalDate.of(2026, 3, 15),
                LocalDateTime.of(2026, 3, 10, 8, 30),
                false,
                rejectionReason,
                closingObservation,
                cancellationReason,
                null,
                requester.getId().orElseThrow(),
                assignedTo.flatMap(User::getId).orElse(null),
                new OriginChannelId(2L),
                requestTypeId,
                List.of(),
                List.of()
        );
        return new RequestSummary(
                request,
                new RequestType(requestTypeId, "Cupo", "Solicitud de cupo", true),
                new OriginChannel(new OriginChannelId(2L), "Correo", true),
                requester,
                assignedTo
        );
    }

    private static RequestDetail sampleDetail() {
        var summary = sampleSummary();
        var historyEntry = new RequestHistory(
                new RequestHistoryId(100L),
                HistoryAction.REGISTERED,
                "Request registered",
                summary.request().getRegistrationDateTime(),
                summary.request().getId(),
                summary.requester().getId().orElseThrow()
        );

        return new RequestDetail(
                summary.request(),
                summary.requestType(),
                summary.originChannel(),
                summary.requester(),
                summary.assignedTo(),
                List.of(new RequestHistoryDetail(historyEntry, summary.requester()))
        );
    }

    private static RequestDetail sampleDetailWithAttendanceHistory() {
        var assignee = sampleUser(15L, "staff15", Role.STAFF);
        var summary = sampleSummary(new RequestTypeId(4L), RequestStatus.ATTENDED, Priority.HIGH,
                "Impacta matrícula en menos de 72 horas.", Optional.of(assignee));
        var registeredEntry = new RequestHistory(
                new RequestHistoryId(100L),
                HistoryAction.REGISTERED,
                "Request registered",
                LocalDateTime.of(2026, 3, 10, 8, 30),
                summary.request().getId(),
                summary.requester().getId().orElseThrow()
        );
        var attendedEntry = new RequestHistory(
                new RequestHistoryId(101L),
                HistoryAction.ATTENDED,
                "Se gestionó el cupo con la coordinación y se notificó al estudiante.",
                LocalDateTime.of(2026, 3, 12, 11, 15),
                summary.request().getId(),
                assignee.getId().orElseThrow(),
                assignee.getId().orElseThrow()
        );

        return new RequestDetail(
                summary.request(),
                summary.requestType(),
                summary.originChannel(),
                summary.requester(),
                summary.assignedTo(),
                List.of(
                        new RequestHistoryDetail(registeredEntry, summary.requester()),
                        new RequestHistoryDetail(attendedEntry, assignee)
                )
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
        return authentication(7L, "jperez", Role.STUDENT);
    }

    private RequestPostProcessor staffAuthentication() {
        return authentication(10L, "staff01", Role.STAFF);
    }

    private RequestPostProcessor adminAuthentication() {
        return authentication(99L, "admin", Role.ADMIN);
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
