package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.request.CreateRequestUseCase;
import co.edu.uniquindio.triage.application.port.in.request.GetRequestDetailQuery;
import co.edu.uniquindio.triage.application.port.in.request.ListRequestsQuery;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.in.request.RequestHistoryDetail;
import co.edu.uniquindio.triage.application.port.in.request.RequestPage;
import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;
import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Role;
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
import co.edu.uniquindio.triage.domain.model.id.RequestHistoryId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.RequestRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.RequestRestMapperImpl;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
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
    private ListRequestsQuery listRequestsQuery;

    @MockitoBean
    private GetRequestDetailQuery getRequestDetailQuery;

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
    void listMustBindFiltersAndReturnPagedContract() throws Exception {
        given(listRequestsQuery.execute(any(), any())).willReturn(new RequestPage<>(List.of(sampleSummary()), 1, 1, 0, 20));

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
        var requester = sampleUser(7L, "jperez", Role.STUDENT);
        var request = new AcademicRequest(
                new RequestId(42L),
                "Necesito un cupo adicional para la materia",
                requester.getId(),
                new OriginChannelId(2L),
                new RequestTypeId(3L),
                LocalDate.of(2026, 3, 15),
                false,
                LocalDateTime.of(2026, 3, 10, 8, 30)
        );
        return new RequestSummary(
                request,
                new RequestType(new RequestTypeId(3L), "Cupo", "Solicitud de cupo", true),
                new OriginChannel(new OriginChannelId(2L), "Correo", true),
                requester,
                Optional.empty()
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
                summary.requester().getId()
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
