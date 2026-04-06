package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.businessrule.*;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.DeactivateBusinessRuleCommand;
import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.BusinessRuleRestMapper;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BusinessRuleController.class)
@ContextConfiguration(classes = {
        BusinessRuleController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        BusinessRuleControllerTest.TestMappersConfiguration.class,
        BusinessRuleControllerTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        BusinessRuleControllerTest.TestMappersConfiguration.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class BusinessRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListBusinessRulesQueryUseCase listBusinessRulesQueryUseCase;

    @MockitoBean
    private GetBusinessRuleQueryUseCase getBusinessRuleQueryUseCase;

    @MockitoBean
    private CreateBusinessRuleUseCase createBusinessRuleUseCase;

    @MockitoBean
    private UpdateBusinessRuleUseCase updateBusinessRuleUseCase;

    @MockitoBean
    private DeactivateBusinessRuleUseCase deactivateBusinessRuleUseCase;

    @Test
    void listRulesMustReturn200ForAdmin() throws Exception {
        BusinessRule rule = sampleRule(1L, "Rule 1");
        given(listBusinessRulesQueryUseCase.list(any())).willReturn(List.of(rule));

        mockMvc.perform(get("/api/v1/business-rules")
                        .with(authentication(Role.ADMIN))
                        .param("active", "true")
                        .param("conditionType", "REQUEST_TYPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Rule 1"));
    }

    @Test
    void listRulesMustReturn200ForStaff() throws Exception {
        mockMvc.perform(get("/api/v1/business-rules")
                        .with(authentication(Role.STAFF)))
                .andExpect(status().isOk());
    }

    @Test
    void listRulesMustReturn403ForStudent() throws Exception {
        mockMvc.perform(get("/api/v1/business-rules")
                        .with(authentication(Role.STUDENT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByIdMustReturn200WhenFound() throws Exception {
        BusinessRule rule = sampleRule(1L, "Rule 1");
        given(getBusinessRuleQueryUseCase.getById(new BusinessRuleId(1L))).willReturn(Optional.of(rule));

        mockMvc.perform(get("/api/v1/business-rules/{id}", 1)
                        .with(authentication(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Rule 1"));
    }

    @Test
    void getByIdMustReturn404WhenNotFound() throws Exception {
        given(getBusinessRuleQueryUseCase.getById(any())).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/business-rules/{id}", 99)
                        .with(authentication(Role.STAFF)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createMustReturn201WhenValid() throws Exception {
        BusinessRule rule = sampleRule(1L, "New Rule");
        given(createBusinessRuleUseCase.create(any())).willReturn(rule);

        mockMvc.perform(post("/api/v1/business-rules")
                        .with(authentication(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New Rule",
                                  "description": "A new rule",
                                  "conditionType": "REQUEST_TYPE",
                                  "conditionValue": "1",
                                  "resultingPriority": "HIGH",
                                  "requestTypeId": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("New Rule"));
    }

    @Test
    void createMustReturn409WhenDuplicateName() throws Exception {
        given(createBusinessRuleUseCase.create(any()))
                .willThrow(new DuplicateCatalogEntryException("Regla de negocio", "New Rule"));

        mockMvc.perform(post("/api/v1/business-rules")
                        .with(authentication(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "New Rule",
                                  "description": "A new rule",
                                  "conditionType": "REQUEST_TYPE",
                                  "conditionValue": "1",
                                  "resultingPriority": "HIGH"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createMustReturn400WhenInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/business-rules")
                        .with(authentication(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "conditionType": "INVALID"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateMustReturn200WhenValid() throws Exception {
        BusinessRule rule = sampleRule(1L, "Updated Rule");
        given(updateBusinessRuleUseCase.update(any())).willReturn(rule);

        mockMvc.perform(put("/api/v1/business-rules/{id}", 1)
                        .with(authentication(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Rule",
                                  "description": "An updated rule",
                                  "conditionType": "REQUEST_TYPE",
                                  "conditionValue": "1",
                                  "resultingPriority": "MEDIUM",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Rule"));
    }

    @Test
    void deleteMustReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/business-rules/{id}", 1)
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isNoContent());

        verify(deactivateBusinessRuleUseCase).deactivate(any());
    }

    @Test
    void writeOperationsMustReturn403ForStaff() throws Exception {
        String validBody = """
                {
                  "name": "Valid Rule",
                  "description": "Desc",
                  "conditionType": "REQUEST_TYPE",
                  "conditionValue": "1",
                  "resultingPriority": "HIGH",
                  "active": true
                }
                """;

        mockMvc.perform(post("/api/v1/business-rules")
                        .with(authentication(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/business-rules/1")
                        .with(authentication(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/business-rules/1")
                        .with(authentication(Role.STAFF)))
                .andExpect(status().isForbidden());
    }

    private BusinessRule sampleRule(Long id, String name) {
        return BusinessRule.reconstitute(
                new BusinessRuleId(id),
                name,
                "Description",
                ConditionType.REQUEST_TYPE,
                "1",
                Priority.HIGH,
                new RequestTypeId(1L),
                true
        );
    }

    private RequestPostProcessor authentication(Role role) {
        var principal = new AuthenticatedUser(1L, "user", role, true);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    @TestConfiguration
    static class TestMappersConfiguration {
        @Bean
        BusinessRuleRestMapper businessRuleRestMapper() {
            return new BusinessRuleRestMapper();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    static class TestApplication {
    }
}
