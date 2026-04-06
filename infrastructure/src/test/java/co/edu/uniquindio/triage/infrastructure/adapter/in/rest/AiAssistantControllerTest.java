package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.ai.AiClassificationSuggestion;
import co.edu.uniquindio.triage.application.port.in.ai.AiGeneratedSummary;
import co.edu.uniquindio.triage.application.port.in.ai.GenerateSummaryUseCase;
import co.edu.uniquindio.triage.application.port.in.ai.SuggestClassificationUseCase;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.AiServiceUnavailableException;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.AiRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.security.AuthenticatedUser;
import co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiAssistantController.class)
@ContextConfiguration(classes = {
        AiAssistantController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        AiAssistantControllerTest.TestMappersConfiguration.class,
        AiAssistantControllerTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        AiAssistantControllerTest.TestMappersConfiguration.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class AiAssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SuggestClassificationUseCase suggestClassificationUseCase;

    @MockitoBean
    private GenerateSummaryUseCase generateSummaryUseCase;

    @Test
    void suggestClassification_WhenStaff_ShouldReturn200() throws Exception {
        var suggestion = new AiClassificationSuggestion("Cupo", Optional.of(new RequestTypeId(1L)), Priority.HIGH, 0.95, "Razonamiento");
        given(suggestClassificationUseCase.execute(any(), any())).willReturn(suggestion);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ai/suggest-classification")
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Necesito un cupo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestedRequestType").value("Cupo"))
                .andExpect(jsonPath("$.suggestedPriority").value("HIGH"));
    }

    @Test
    void suggestClassification_WhenStudent_ShouldReturn403() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ai/suggest-classification")
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Necesito un cupo\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void suggestClassification_WhenAiUnavailable_ShouldReturn503() throws Exception {
        given(suggestClassificationUseCase.execute(any(), any())).willThrow(new AiServiceUnavailableException("Indisponible"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/ai/suggest-classification")
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\": \"Necesito un cupo\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void summarize_WhenAdmin_ShouldReturn200() throws Exception {
        var requestId = RequestId.of(100L);
        var summary = new AiGeneratedSummary(requestId, "Resumen generado", LocalDateTime.now());
        given(generateSummaryUseCase.execute(any(), any())).willReturn(summary);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ai/summarize/100")
                        .with(adminAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(100))
                .andExpect(jsonPath("$.summary").value("Resumen generado"));
    }

    @Test
    void summarize_WhenStudent_ShouldReturn403() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ai/summarize/100")
                        .with(studentAuthentication()))
                .andExpect(status().isForbidden());
    }

    @Test
    void summarize_WhenRequestNotFound_ShouldReturn404() throws Exception {
        given(generateSummaryUseCase.execute(any(), any())).willThrow(new RequestNotFoundException(RequestId.of(999L)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ai/summarize/999")
                        .with(adminAuthentication()))
                .andExpect(status().isNotFound());
    }

    @Test
    void summarize_WhenAiUnavailable_ShouldReturn503() throws Exception {
        given(generateSummaryUseCase.execute(any(), any())).willThrow(new AiServiceUnavailableException("Indisponible"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/ai/summarize/100")
                        .with(adminAuthentication()))
                .andExpect(status().isServiceUnavailable());
    }

    private RequestPostProcessor adminAuthentication() {
        return authentication(99L, "admin", Role.ADMIN);
    }

    private RequestPostProcessor staffAuthentication() {
        return authentication(10L, "staff01", Role.STAFF);
    }

    private RequestPostProcessor studentAuthentication() {
        return authentication(7L, "jperez", Role.STUDENT);
    }

    private RequestPostProcessor authentication(long id, String username, Role role) {
        var principal = new AuthenticatedUser(id, username, role, true);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    @TestConfiguration
    static class TestMappersConfiguration {
        @Bean
        AiRestMapper aiRestMapper() {
            return new AiRestMapper();
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
