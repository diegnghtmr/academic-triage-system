package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.catalog.CreateOriginChannelUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.CreateRequestTypeUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.GetOriginChannelQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.GetOriginChannelVersionUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.GetRequestTypeQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.GetRequestTypeVersionUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.ListOriginChannelsQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.ListRequestTypesQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.UpdateOriginChannelUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.UpdateRequestTypeUseCase;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.CatalogRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
@ContextConfiguration(classes = {
        CatalogController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        NoopLoadUserAuthPortTestConfiguration.class,
        CatalogControllerETagTest.TestMappersConfiguration.class,
        CatalogControllerETagTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        NoopLoadUserAuthPortTestConfiguration.class,
        CatalogControllerETagTest.TestMappersConfiguration.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class CatalogControllerETagTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private ListRequestTypesQuery listRequestTypesQuery;
    @MockitoBean private GetRequestTypeQuery getRequestTypeQuery;
    @MockitoBean private GetRequestTypeVersionUseCase getRequestTypeVersionUseCase;
    @MockitoBean private CreateRequestTypeUseCase createRequestTypeUseCase;
    @MockitoBean private UpdateRequestTypeUseCase updateRequestTypeUseCase;
    @MockitoBean private ListOriginChannelsQuery listOriginChannelsQuery;
    @MockitoBean private GetOriginChannelQuery getOriginChannelQuery;
    @MockitoBean private GetOriginChannelVersionUseCase getOriginChannelVersionUseCase;
    @MockitoBean private CreateOriginChannelUseCase createOriginChannelUseCase;
    @MockitoBean private UpdateOriginChannelUseCase updateOriginChannelUseCase;
    @MockitoBean private HttpIdempotencySupport httpIdempotencySupport;

    @BeforeEach
    void configureIdempotencyPassThrough() {
        willAnswer(inv -> ((java.util.function.Supplier<?>) inv.getArgument(7)).get())
                .given(httpIdempotencySupport).execute(any(), any(), any(), any(), any(), any(), any(), any());
    }

    // -- RequestType ETag tests --

    @Test
    void getRequestTypeByIdMustEmitETag() throws Exception {
        given(getRequestTypeQuery.execute(any(), any())).willReturn(sampleRequestType(1L));
        given(getRequestTypeVersionUseCase.getVersionById(any())).willReturn(Optional.of(4L));

        mockMvc.perform(get("/api/v1/catalogs/request-types/{id}", 1).with(admin()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"4\""));
    }

    @Test
    void putRequestTypeMustReturn428WhenNoIfMatch() throws Exception {
        given(getRequestTypeVersionUseCase.getVersionById(any())).willReturn(Optional.of(1L));

        mockMvc.perform(put("/api/v1/catalogs/request-types/{id}", 1)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestTypeBody()))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.status").value(428));
    }

    @Test
    void putRequestTypeMustReturn412WhenStaleVersion() throws Exception {
        given(getRequestTypeVersionUseCase.getVersionById(any())).willReturn(Optional.of(5L));

        mockMvc.perform(put("/api/v1/catalogs/request-types/{id}", 1)
                        .with(admin())
                        .header("If-Match", "\"2\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestTypeBody()))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.status").value(412));
    }

    @Test
    void putRequestTypeMustReturn200WhenCorrectVersion() throws Exception {
        given(getRequestTypeVersionUseCase.getVersionById(any())).willReturn(Optional.of(2L));
        given(updateRequestTypeUseCase.execute(any(), any())).willReturn(sampleRequestType(1L));

        mockMvc.perform(put("/api/v1/catalogs/request-types/{id}", 1)
                        .with(admin())
                        .header("If-Match", "\"2\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestTypeBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // -- OriginChannel ETag tests --

    @Test
    void getOriginChannelByIdMustEmitETag() throws Exception {
        given(getOriginChannelQuery.execute(any(), any())).willReturn(sampleOriginChannel(1L));
        given(getOriginChannelVersionUseCase.getVersionById(any())).willReturn(Optional.of(7L));

        mockMvc.perform(get("/api/v1/catalogs/origin-channels/{id}", 1).with(admin()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"7\""));
    }

    @Test
    void putOriginChannelMustReturn428WhenNoIfMatch() throws Exception {
        given(getOriginChannelVersionUseCase.getVersionById(any())).willReturn(Optional.of(1L));

        mockMvc.perform(put("/api/v1/catalogs/origin-channels/{id}", 1)
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOriginChannelBody()))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.status").value(428));
    }

    @Test
    void putOriginChannelMustReturn412WhenStaleVersion() throws Exception {
        given(getOriginChannelVersionUseCase.getVersionById(any())).willReturn(Optional.of(3L));

        mockMvc.perform(put("/api/v1/catalogs/origin-channels/{id}", 1)
                        .with(admin())
                        .header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOriginChannelBody()))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.status").value(412));
    }

    @Test
    void putOriginChannelMustReturn200WhenCorrectVersion() throws Exception {
        given(getOriginChannelVersionUseCase.getVersionById(any())).willReturn(Optional.of(1L));
        given(updateOriginChannelUseCase.execute(any(), any())).willReturn(sampleOriginChannel(1L));

        mockMvc.perform(put("/api/v1/catalogs/origin-channels/{id}", 1)
                        .with(admin())
                        .header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOriginChannelBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    private RequestType sampleRequestType(Long id) {
        return new RequestType(new RequestTypeId(id), "Certificado", "Desc", true);
    }

    private OriginChannel sampleOriginChannel(Long id) {
        return new OriginChannel(new OriginChannelId(id), "Web", true);
    }

    private String validRequestTypeBody() {
        return """
                {"name": "Certificado", "description": "Desc", "active": true}
                """;
    }

    private String validOriginChannelBody() {
        return """
                {"name": "Web", "active": true}
                """;
    }

    private RequestPostProcessor admin() {
        var principal = new AuthenticatedUser(1L, "admin", Role.ADMIN, true);
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
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    static class TestApplication {
    }
}
