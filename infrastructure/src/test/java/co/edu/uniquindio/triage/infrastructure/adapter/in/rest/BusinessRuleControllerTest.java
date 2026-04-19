package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.exception.MissingIdempotencyKeyException;
import co.edu.uniquindio.triage.application.port.in.businessrule.*;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.DeactivateBusinessRuleCommand;
import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.BusinessRuleRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.CatalogRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.ETagSupport;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.HttpIdempotencySupport;
import co.edu.uniquindio.triage.infrastructure.adapter.out.security.AuthenticatedUser;
import co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration;
import co.edu.uniquindio.triage.infrastructure.testsupport.NoopLoadUserAuthPortTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static co.edu.uniquindio.triage.infrastructure.adapter.in.rest.BusinessRuleConditionTypeParser.LEGACY_IMPACT_LEVEL_REJECTED;

@WebMvcTest(BusinessRuleController.class)
@ContextConfiguration(classes = {
        BusinessRuleController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        NoopLoadUserAuthPortTestConfiguration.class,
        BusinessRuleControllerTest.TestMappersConfiguration.class,
        BusinessRuleControllerTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        NoopLoadUserAuthPortTestConfiguration.class,
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

    @MockitoBean
    private GetBusinessRuleVersionUseCase getBusinessRuleVersionUseCase;

    @MockitoBean
    private HttpIdempotencySupport httpIdempotencySupport;

    @BeforeEach
    void configureIdempotencyPassThrough() {
        willAnswer(inv -> ((java.util.function.Supplier<?>) inv.getArgument(7)).get())
                .given(httpIdempotencySupport).execute(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void listRulesMustReturn200ForAdmin() throws Exception {
        var rule = sampleRule(1L, "Rule 1");
        given(listBusinessRulesQueryUseCase.list(any())).willReturn(List.of(new BusinessRuleView(rule, null)));

        mockMvc.perform(get("/api/v1/business-rules")
                        .with(authentication(Role.ADMIN))
                        .param("active", "true")
                        .param("conditionType", "REQUEST_TYPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Rule 1"));
    }

    @Test
    void listRulesMustExposeHydratedRequestTypeWhenPresent() throws Exception {
        var rule = sampleRule(1L, "Rule 1");
        var rt = mock(RequestType.class);
        given(rt.getId()).willReturn(new RequestTypeId(1L));
        given(rt.getName()).willReturn("Certificado");
        given(rt.getDescription()).willReturn("D");
        given(rt.isActive()).willReturn(true);
        given(listBusinessRulesQueryUseCase.list(any())).willReturn(List.of(new BusinessRuleView(rule, rt)));

        mockMvc.perform(get("/api/v1/business-rules").with(authentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestType.id").value(1))
                .andExpect(jsonPath("$[0].requestType.name").value("Certificado"));
    }

    @Test
    void listRulesMustReturn400WhenConditionTypeIsLegacyImpactLevel() throws Exception {
        mockMvc.perform(get("/api/v1/business-rules")
                        .with(authentication(Role.ADMIN))
                        .param("conditionType", "IMPACT_LEVEL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(LEGACY_IMPACT_LEVEL_REJECTED));
    }

    @Test
    void createMustReturn400WhenConditionTypeIsLegacyImpactLevel() throws Exception {
        mockMvc.perform(post("/api/v1/business-rules")
                        .with(authentication(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Legacy",
                                  "description": "x",
                                  "conditionType": "IMPACT_LEVEL",
                                  "conditionValue": "HIGH",
                                  "resultingPriority": "HIGH"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(LEGACY_IMPACT_LEVEL_REJECTED));
    }

    @Test
    void updateMustReturn400WhenConditionTypeIsLegacyImpactLevel() throws Exception {
        given(getBusinessRuleVersionUseCase.getVersionById(any())).willReturn(Optional.of(0L));

        mockMvc.perform(put("/api/v1/business-rules/{id}", 1)
                        .with(authentication(Role.ADMIN))
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Legacy",
                                  "description": "x",
                                  "conditionType": "IMPACT_LEVEL",
                                  "conditionValue": "HIGH",
                                  "resultingPriority": "HIGH",
                                  "active": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(LEGACY_IMPACT_LEVEL_REJECTED));
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
        given(getBusinessRuleQueryUseCase.getById(new BusinessRuleId(1L))).willReturn(Optional.of(new BusinessRuleView(rule, null)));

        mockMvc.perform(get("/api/v1/business-rules/{id}", 1)
                        .with(authentication(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Rule 1"));
    }

    @Test
    void getByIdMustReturn200WithHydratedRequestTypeFromRealCatalogProjection() throws Exception {
        var homologacion = new RequestType(new RequestTypeId(3L), "Homologación", "Solicitud de homologación de asignaturas", true);
        var rule = BusinessRule.reconstitute(
                new BusinessRuleId(42L),
                "Regla catalogada",
                "Descripción",
                ConditionType.REQUEST_TYPE,
                "3",
                Priority.HIGH,
                new RequestTypeId(3L),
                true);
        given(getBusinessRuleQueryUseCase.getById(new BusinessRuleId(42L)))
                .willReturn(Optional.of(new BusinessRuleView(rule, homologacion)));

        mockMvc.perform(get("/api/v1/business-rules/{ruleId}", 42).with(authentication(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("Regla catalogada"))
                .andExpect(jsonPath("$.conditionType").value("REQUEST_TYPE"))
                .andExpect(jsonPath("$.conditionValue").value("3"))
                .andExpect(jsonPath("$.requestType.id").value(3))
                .andExpect(jsonPath("$.requestType.name").value("Homologación"))
                .andExpect(jsonPath("$.requestType.description").value("Solicitud de homologación de asignaturas"))
                .andExpect(jsonPath("$.requestType.active").value(true));
    }

    @Test
    void getByIdMustOmitRequestTypeInJsonWhenViewHasNoCatalogHydration() throws Exception {
        var rule = BusinessRule.reconstitute(
                new BusinessRuleId(7L),
                "Solo plazo",
                "Sin vínculo a catálogo",
                ConditionType.DEADLINE,
                "5",
                Priority.MEDIUM,
                null,
                true);
        given(getBusinessRuleQueryUseCase.getById(new BusinessRuleId(7L)))
                .willReturn(Optional.of(new BusinessRuleView(rule, null)));

        mockMvc.perform(get("/api/v1/business-rules/{ruleId}", 7).with(authentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.conditionType").value("DEADLINE"))
                .andExpect(jsonPath("$.requestType").doesNotExist());
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
        var rule = sampleRule(1L, "New Rule");
        given(createBusinessRuleUseCase.create(any())).willReturn(new BusinessRuleView(rule, null));

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
        var rule = sampleRule(1L, "Updated Rule");
        given(updateBusinessRuleUseCase.update(any())).willReturn(new BusinessRuleView(rule, null));
        given(getBusinessRuleVersionUseCase.getVersionById(any())).willReturn(Optional.of(0L));

        mockMvc.perform(put("/api/v1/business-rules/{id}", 1)
                        .with(authentication(Role.ADMIN))
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Rule",
                                  "description": "An updated rule",
                                  "conditionType": "REQUEST_TYPE",
                                  "conditionValue": "1",
                                  "resultingPriority": "MEDIUM",
                                  "requestTypeId": 1,
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Rule"));
    }

    @Test
    void deleteMustReturn204() throws Exception {
        given(getBusinessRuleVersionUseCase.getVersionById(any())).willReturn(Optional.of(0L));

        mockMvc.perform(delete("/api/v1/business-rules/{id}", 1)
                        .with(authentication(Role.ADMIN))
                        .header("If-Match", "\"0\""))
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
                  "requestTypeId": 1,
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

    @Test
    void getByIdMustEmitETagHeaderWhenVersionExists() throws Exception {
        var rule = sampleRule(1L, "Rule 1");
        given(getBusinessRuleQueryUseCase.getById(new BusinessRuleId(1L))).willReturn(Optional.of(new BusinessRuleView(rule, null)));
        given(getBusinessRuleVersionUseCase.getVersionById(new BusinessRuleId(1L))).willReturn(Optional.of(7L));

        mockMvc.perform(get("/api/v1/business-rules/{id}", 1)
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"7\""));
    }

    @Test
    void updateMustReturn428WhenIfMatchHeaderIsMissing() throws Exception {
        given(getBusinessRuleVersionUseCase.getVersionById(any())).willReturn(Optional.of(3L));

        mockMvc.perform(put("/api/v1/business-rules/{id}", 1)
                        .with(authentication(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rule",
                                  "description": "Desc",
                                  "conditionType": "REQUEST_TYPE",
                                  "conditionValue": "1",
                                  "resultingPriority": "HIGH",
                                  "active": true
                                }
                                """))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.status").value(428));
    }

    @Test
    void updateMustReturn412WhenVersionMismatch() throws Exception {
        given(getBusinessRuleVersionUseCase.getVersionById(any())).willReturn(Optional.of(5L));

        mockMvc.perform(put("/api/v1/business-rules/{id}", 1)
                        .with(authentication(Role.ADMIN))
                        .header("If-Match", "\"3\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rule",
                                  "description": "Desc",
                                  "conditionType": "REQUEST_TYPE",
                                  "conditionValue": "1",
                                  "resultingPriority": "HIGH",
                                  "active": true
                                }
                                """))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.status").value(412));
    }

    @Test
    void deleteMustReturn428WhenIfMatchHeaderIsMissing() throws Exception {
        given(getBusinessRuleVersionUseCase.getVersionById(any())).willReturn(Optional.of(3L));

        mockMvc.perform(delete("/api/v1/business-rules/{id}", 1)
                        .with(authentication(Role.ADMIN)))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.status").value(428));
    }

    @Test
    void deleteMustReturn412WhenVersionMismatch() throws Exception {
        given(getBusinessRuleVersionUseCase.getVersionById(any())).willReturn(Optional.of(5L));

        mockMvc.perform(delete("/api/v1/business-rules/{id}", 1)
                        .with(authentication(Role.ADMIN))
                        .header("If-Match", "\"3\""))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.status").value(412));
    }

    @Test
    void createMustReturn400WhenIdempotencyKeyIsMissing() throws Exception {
        org.mockito.Mockito.reset(httpIdempotencySupport);
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new MissingIdempotencyKeyException());

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createMustReturn422WhenFingerprintMismatch() throws Exception {
        org.mockito.Mockito.reset(httpIdempotencySupport);
        given(httpIdempotencySupport.execute(any(), any(), any(), any(), any(), any(), any(), any()))
                .willThrow(new co.edu.uniquindio.triage.application.exception.IdempotencyFingerprintMismatchException());

        mockMvc.perform(post("/api/v1/business-rules")
                        .with(authentication(Role.ADMIN))
                        .header("Idempotency-Key", "key-br-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Regla diferente al original",
                                  "conditionType": "DEADLINE",
                                  "conditionValue": "5",
                                  "resultingPriority": "LOW"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
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
        CatalogRestMapper catalogRestMapper() {
            return new CatalogRestMapper();
        }

        @Bean
        BusinessRuleRestMapper businessRuleRestMapper(CatalogRestMapper catalogRestMapper) {
            return new BusinessRuleRestMapper(catalogRestMapper);
        }

        @Bean
        ETagSupport eTagSupport() {
            return new ETagSupport();
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    static class TestApplication {
    }
}
