package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.command.request.AddInternalNoteCommand;
import co.edu.uniquindio.triage.application.port.in.command.request.GetRequestDetailQueryModel;
import co.edu.uniquindio.triage.application.port.in.request.*;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.*;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.AddInternalNoteRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.request.HistoryEntryResponse;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        RequestControllerHistoryTest.TestApplication.class,
        AuthenticatedActorMapper.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class RequestControllerHistoryTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddInternalNoteUseCase addInternalNoteUseCase;

    @MockitoBean
    private LoadUserAuthPort loadUserAuthPort;

    @MockitoBean
    private GetRequestDetailQuery getRequestDetailQuery;

    @MockitoBean
    private GetPrioritySuggestionQuery getPrioritySuggestionQuery;

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
    @DisplayName("GIVEN student is the owner WHEN gets history THEN returns 200 with actor data")
    void studentOwnHistoryReturns200() throws Exception {
        RequestId requestId = new RequestId(1L);
        User student = sampleUser(1L, "student1", Role.STUDENT);
        
        var historyEntry = new RequestHistory(null, HistoryAction.REGISTERED, "Initial registration", LocalDateTime.now(), requestId, student.getId().orElseThrow());
        var historyDetail = new RequestHistoryDetail(historyEntry, student);
        RequestDetail detail = createMockDetail(requestId, student, List.of(historyDetail));

        var responseEntry = new HistoryEntryResponse(
                50L, 
                HistoryAction.REGISTERED, 
                "Initial registration", 
                historyEntry.getTimestamp(),
                new co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user.UserResponse(1L, "student1", "First", "Last", "ID-1", "student1@test.com", Role.STUDENT.name(), true)
        );

        when(getRequestDetailQuery.execute(any(), any())).thenReturn(detail);
        when(requestRestMapper.toResponse(any(RequestHistoryDetail.class))).thenReturn(responseEntry);

        mockMvc.perform(get("/api/v1/requests/1/history")
                        .with(authentication(Role.STUDENT, "student1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("REGISTERED"))
                .andExpect(jsonPath("$[0].performedBy.id").value(1))
                .andExpect(jsonPath("$[0].performedBy.username").value("student1"));
    }

    @Test
    @DisplayName("GIVEN student is NOT the owner WHEN gets history THEN returns 403")
    void studentOtherHistoryReturns403() throws Exception {
        RequestId requestId = new RequestId(1L);
        User owner = sampleUser(1L, "student1", Role.STUDENT);
        RequestDetail detail = createMockDetail(requestId, owner, List.of());

        when(getRequestDetailQuery.execute(any(), any())).thenReturn(detail);

        mockMvc.perform(get("/api/v1/requests/1/history")
                        .with(authentication(Role.STUDENT, "student2")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GIVEN staff member WHEN posts internal note THEN returns 201 with body and actor")
    void staffPostNoteReturns201() throws Exception {
        var userId = new UserId(10L);
        var performer = sampleUser(10L, "staff1", Role.STAFF);
        var entry = new RequestHistoryDetail(
                new RequestHistory(null, HistoryAction.INTERNAL_NOTE, "This is an internal note", LocalDateTime.now(), new RequestId(1L), userId),
                performer
        );
        
        var performerResponse = new co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.user.UserResponse(
                10L, "staff1", "First", "Last", "ID-10", "staff1@test.com", Role.STAFF.name(), true
        );
        var responseBody = new HistoryEntryResponse(100L, HistoryAction.INTERNAL_NOTE, "This is an internal note", LocalDateTime.now(), performerResponse);

        when(addInternalNoteUseCase.addInternalNote(any())).thenReturn(entry);
        when(requestRestMapper.toResponse(any(RequestHistoryDetail.class))).thenReturn(responseBody);

        mockMvc.perform(post("/api/v1/requests/1/history")
                        .with(authentication(Role.STAFF, "staff1"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observations\": \"This is an internal note\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.action").value("INTERNAL_NOTE"))
                .andExpect(jsonPath("$.observations").value("This is an internal note"))
                .andExpect(jsonPath("$.performedBy.id").value(10))
                .andExpect(jsonPath("$.performedBy.username").value("staff1"));
    }

    @Test
    @DisplayName("GIVEN admin member WHEN gets any history THEN returns 200")
    void adminGetsAnyHistoryReturns200() throws Exception {
        RequestId requestId = new RequestId(1L);
        User owner = sampleUser(1L, "student1", Role.STUDENT);
        RequestDetail detail = createMockDetail(requestId, owner, List.of());

        when(getRequestDetailQuery.execute(any(), any())).thenReturn(detail);

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

    private RequestDetail createMockDetail(RequestId id, User requester, List<RequestHistoryDetail> history) {
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
        
        return new RequestDetail(request, type, channel, requester, Optional.empty(), history);
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
