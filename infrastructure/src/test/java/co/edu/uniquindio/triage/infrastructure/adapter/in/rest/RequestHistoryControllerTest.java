package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.AddInternalNoteCommand;
import co.edu.uniquindio.triage.application.port.in.command.request.GetRequestDetailQueryModel;
import co.edu.uniquindio.triage.application.port.in.request.*;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.*;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.AddInternalNoteRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.RequestRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.security.AuthenticatedUser;
import co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestController.class)
@ContextConfiguration(classes = {
        RequestController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        RequestHistoryControllerTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        AuthenticatedActorMapper.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class RequestHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddInternalNoteUseCase addInternalNoteUseCase;

    @MockitoBean
    private GetRequestHistoryQuery getRequestHistoryQuery;

    @MockitoBean
    private GetRequestDetailQuery getRequestDetailQuery;

    @MockitoBean private CreateRequestUseCase createRequestUseCase;
    @MockitoBean private ClassifyRequestUseCase classifyRequestUseCase;
    @MockitoBean private PrioritizeRequestUseCase prioritizeRequestUseCase;
    @MockitoBean private AssignRequestUseCase assignRequestUseCase;
    @MockitoBean private AttendRequestUseCase attendRequestUseCase;
    @MockitoBean private CloseRequestUseCase closeRequestUseCase;
    @MockitoBean private CancelRequestUseCase cancelRequestUseCase;
    @MockitoBean private RejectRequestUseCase rejectRequestUseCase;
    @MockitoBean private ListRequestsQuery listRequestsQuery;
    
    @MockitoBean 
    private RequestRestMapper requestRestMapper;

    @Autowired
    private AuthenticatedActorMapper authenticatedActorMapper;

    @BeforeEach
    void setUp() {
        when(requestRestMapper.toDetailQuery(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return new GetRequestDetailQueryModel(new RequestId(id));
        });
        when(requestRestMapper.toCommand(anyLong(), any(AddInternalNoteRequest.class), any(UserId.class))).thenAnswer(inv -> {
            Long requestId = inv.getArgument(0);
            UserId authorId = inv.getArgument(2);
            return new AddInternalNoteCommand(new RequestId(requestId), "Observation", authorId);
        });
    }

    @Test
    @DisplayName("GIVEN student is the owner WHEN gets history THEN returns 200")
    void studentOwnHistoryReturns200() throws Exception {
        RequestId requestId = new RequestId(1L);
        User student = sampleUser(1L, "student1", Role.STUDENT);
        RequestDetail detail = createMockDetail(requestId, student);

        when(getRequestDetailQuery.execute(any(), any())).thenReturn(detail);
        when(getRequestHistoryQuery.getRequestHistory(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/requests/1/history")
                        .with(authentication(Role.STUDENT, "student1")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GIVEN student is NOT the owner WHEN gets history THEN returns 403")
    void studentOtherHistoryReturns403() throws Exception {
        RequestId requestId = new RequestId(1L);
        User owner = sampleUser(1L, "student1", Role.STUDENT);
        RequestDetail detail = createMockDetail(requestId, owner);

        when(getRequestDetailQuery.execute(any(), any())).thenReturn(detail);

        mockMvc.perform(get("/api/v1/requests/1/history")
                        .with(authentication(Role.STUDENT, "student2")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GIVEN staff member WHEN posts internal note THEN returns 201")
    void staffPostNoteReturns201() throws Exception {
        mockMvc.perform(post("/api/v1/requests/1/history")
                        .with(authentication(Role.STAFF, "staff1"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observations\": \"This is an internal note\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GIVEN admin member WHEN gets any history THEN returns 200")
    void adminGetsAnyHistoryReturns200() throws Exception {
        RequestId requestId = new RequestId(1L);
        User owner = sampleUser(1L, "student1", Role.STUDENT);
        RequestDetail detail = createMockDetail(requestId, owner);

        when(getRequestDetailQuery.execute(any(), any())).thenReturn(detail);
        when(getRequestHistoryQuery.getRequestHistory(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/requests/1/history")
                        .with(authentication(Role.ADMIN, "admin1")))
                .andExpect(status().isOk());
    }

    private User sampleUser(Long id, String username, Role role) {
        return User.reconstitute(
                new UserId(id),
                new Username(username),
                "First",
                "Last",
                new PasswordHash("hash"),
                new Identification("12345"),
                new Email(username + "@test.com"),
                role,
                true
        );
    }

    private RequestDetail createMockDetail(RequestId id, User requester) {
        AcademicRequest request = new AcademicRequest(
                id,
                "Description test length long enough",
                requester.getId().orElseThrow(),
                new OriginChannelId(1L),
                new RequestTypeId(1L),
                null,
                false,
                LocalDateTime.now()
        );
        RequestType type = new RequestType(new RequestTypeId(1L), "Type", "Desc", true);
        OriginChannel channel = new OriginChannel(new OriginChannelId(1L), "Channel", true);
        
        return new RequestDetail(request, type, channel, requester, Optional.empty(), List.of());
    }

    private RequestPostProcessor authentication(Role role, String username) {
        var principal = new AuthenticatedUser(1L, username, role, true);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    static class TestApplication {
    }
}
