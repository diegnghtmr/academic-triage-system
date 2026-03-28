package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.catalog.CreateOriginChannelUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.CreateRequestTypeUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.GetOriginChannelQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.GetRequestTypeQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.ListOriginChannelsQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.ListRequestTypesQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.UpdateOriginChannelUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.UpdateRequestTypeUseCase;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.advice.GlobalExceptionHandler;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.CatalogRestMapper;
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
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Comparator;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
@ContextConfiguration(classes = {
        CatalogController.class,
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        CatalogControllerTest.TestMappersConfiguration.class,
        CatalogControllerTest.TestApplication.class
})
@Import({
        GlobalExceptionHandler.class,
        SecurityConfiguration.class,
        CatalogControllerTest.TestMappersConfiguration.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=12345678901234567890123456789012",
        "app.jwt.expiration-ms=86400000"
})
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @MockitoBean
    private ListRequestTypesQuery listRequestTypesQuery;

    @MockitoBean
    private GetRequestTypeQuery getRequestTypeQuery;

    @MockitoBean
    private CreateRequestTypeUseCase createRequestTypeUseCase;

    @MockitoBean
    private UpdateRequestTypeUseCase updateRequestTypeUseCase;

    @MockitoBean
    private ListOriginChannelsQuery listOriginChannelsQuery;

    @MockitoBean
    private GetOriginChannelQuery getOriginChannelQuery;

    @MockitoBean
    private CreateOriginChannelUseCase createOriginChannelUseCase;

    @MockitoBean
    private UpdateOriginChannelUseCase updateOriginChannelUseCase;

    @Test
    void listRequestTypesMustReturn200AndDefaultActiveFilterToTrue() throws Exception {
        given(listRequestTypesQuery.execute(any(), any())).willReturn(List.of(
                new RequestType(new RequestTypeId(3L), "Homologación", "Reconocimiento externo", true)
        ));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/catalogs/request-types")
                        .with(staffAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].name").value("Homologación"))
                .andExpect(jsonPath("$[0].description").value("Reconocimiento externo"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(listRequestTypesQuery).execute(
                argThat(query -> query.active().filter(Boolean.TRUE::equals).isPresent()),
                any()
        );
    }

    @Test
    void getCatalogRoutesMustRequireAuthentication() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/catalogs/request-types"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.containsString(MediaType.APPLICATION_PROBLEM_JSON_VALUE)))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void getRequestTypeMustReturn200WhenTypeExists() throws Exception {
        given(getRequestTypeQuery.execute(any(), any())).willReturn(
                new RequestType(new RequestTypeId(21L), "Reintegro", "Solicitud de reintegro", true)
        );

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/catalogs/request-types/{typeId}", 21L)
                        .with(staffAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.name").value("Reintegro"))
                .andExpect(jsonPath("$.description").value("Solicitud de reintegro"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createRequestTypeMustReturn201AndBindCommand() throws Exception {
        given(createRequestTypeUseCase.execute(any(), any())).willReturn(
                new RequestType(new RequestTypeId(21L), "Reintegro", "Solicitud de reintegro", true)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/catalogs/request-types")
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  Reintegro  ",
                                  "description": "  Solicitud de reintegro  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/catalogs/request-types/21"))
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.name").value("Reintegro"));

        verify(createRequestTypeUseCase).execute(
                argThat(command -> "Reintegro".equals(command.name())
                        && "Solicitud de reintegro".equals(command.description())),
                any()
        );
    }

    @Test
    void createRequestTypeMustReturn400WhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/catalogs/request-types")
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void createRequestTypeMustReturn403ForNonAdminUsers() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/catalogs/request-types")
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Reintegro"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(createRequestTypeUseCase);
    }

    @Test
    void createRequestTypeMustReturn409WhenNameIsDuplicated() throws Exception {
        given(createRequestTypeUseCase.execute(any(), any()))
                .willThrow(new DuplicateCatalogEntryException("tipo de solicitud", "Reintegro"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/catalogs/request-types")
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Reintegro",
                                  "description": "Solicitud duplicada"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Ya existe un tipo de solicitud con nombre 'Reintegro'"));
    }

    @Test
    void getRequestTypeMustReturn404WhenTypeDoesNotExist() throws Exception {
        given(getRequestTypeQuery.execute(any(), any())).willThrow(new EntityNotFoundException("RequestType", "id", 99L));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/catalogs/request-types/{typeId}", 99L)
                        .with(staffAuthentication()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateRequestTypeMustReturn409WhenNameIsDuplicated() throws Exception {
        given(updateRequestTypeUseCase.execute(any(), any()))
                .willThrow(new DuplicateCatalogEntryException("tipo de solicitud", "Reintegro"));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/catalogs/request-types/{typeId}", 21L)
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Reintegro",
                                  "description": "Solicitud actualizada"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Ya existe un tipo de solicitud con nombre 'Reintegro'"));
    }

    @Test
    void updateRequestTypeMustReturn200AndBindCommand() throws Exception {
        given(updateRequestTypeUseCase.execute(any(), any())).willReturn(
                new RequestType(new RequestTypeId(21L), "Reintegro actualizado", "Solicitud actualizada", true)
        );

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/catalogs/request-types/{typeId}", 21L)
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  Reintegro actualizado  ",
                                  "description": "  Solicitud actualizada  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.name").value("Reintegro actualizado"))
                .andExpect(jsonPath("$.description").value("Solicitud actualizada"))
                .andExpect(jsonPath("$.active").value(true));

        verify(updateRequestTypeUseCase).execute(
                argThat(command -> command.requestTypeId().value().equals(21L)
                        && "Reintegro actualizado".equals(command.name())
                        && "Solicitud actualizada".equals(command.description())),
                any()
        );
    }

    @Test
    void updateRequestTypeMustReturn403ForNonAdminUsers() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/catalogs/request-types/{typeId}", 21L)
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Reintegro actualizado",
                                  "description": "Solicitud actualizada"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(updateRequestTypeUseCase);
    }

    @Test
    void listOriginChannelsMustReturn200AndDefaultActiveFilterToTrue() throws Exception {
        given(listOriginChannelsQuery.execute(any(), any())).willReturn(List.of(
                new OriginChannel(new OriginChannelId(2L), "Correo", true)
        ));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/catalogs/origin-channels")
                        .with(studentAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Correo"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(listOriginChannelsQuery).execute(
                argThat(query -> query.active().filter(Boolean.TRUE::equals).isPresent()),
                any()
        );
    }

    @Test
    void createOriginChannelMustReturn201AndBody() throws Exception {
        given(createOriginChannelUseCase.execute(any(), any())).willReturn(
                new OriginChannel(new OriginChannelId(8L), "Llamada", true)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/catalogs/origin-channels")
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  Llamada  "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/catalogs/origin-channels/8"))
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.name").value("Llamada"));

        verify(createOriginChannelUseCase).execute(argThat(command -> "Llamada".equals(command.name())), any());
    }

    @Test
    void createOriginChannelMustReturn403ForNonAdminUsers() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/catalogs/origin-channels")
                        .with(staffAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Llamada"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(createOriginChannelUseCase);
    }

    @Test
    void createOriginChannelMustReturn409WhenNameIsDuplicated() throws Exception {
        given(createOriginChannelUseCase.execute(any(), any()))
                .willThrow(new DuplicateCatalogEntryException("canal de origen", "Correo"));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/catalogs/origin-channels")
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Correo"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Ya existe un canal de origen con nombre 'Correo'"));
    }

    @Test
    void updateOriginChannelMustReturn400WhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/catalogs/origin-channels/{channelId}", 8L)
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void updateOriginChannelMustReturn403ForNonAdminUsers() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/catalogs/origin-channels/{channelId}", 8L)
                        .with(studentAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Llamada"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(updateOriginChannelUseCase);
    }

    @Test
    void getOriginChannelMustReturn404WhenChannelDoesNotExist() throws Exception {
        given(getOriginChannelQuery.execute(any(), any())).willThrow(new EntityNotFoundException("OriginChannel", "id", 404L));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/catalogs/origin-channels/{channelId}", 404L)
                        .with(staffAuthentication()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getOriginChannelMustReturn200WhenChannelExists() throws Exception {
        given(getOriginChannelQuery.execute(any(), any())).willReturn(
                new OriginChannel(new OriginChannelId(8L), "Correo", true)
        );

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/catalogs/origin-channels/{channelId}", 8L)
                        .with(studentAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.name").value("Correo"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void updateOriginChannelMustReturn409WhenNameIsDuplicated() throws Exception {
        given(updateOriginChannelUseCase.execute(any(), any()))
                .willThrow(new DuplicateCatalogEntryException("canal de origen", "Correo"));

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/catalogs/origin-channels/{channelId}", 8L)
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Correo"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Ya existe un canal de origen con nombre 'Correo'"));
    }

    @Test
    void updateOriginChannelMustReturn200AndBindCommand() throws Exception {
        given(updateOriginChannelUseCase.execute(any(), any())).willReturn(
                new OriginChannel(new OriginChannelId(8L), "Llamada institucional", true)
        );

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/catalogs/origin-channels/{channelId}", 8L)
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  Llamada institucional  "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.name").value("Llamada institucional"))
                .andExpect(jsonPath("$.active").value(true));

        verify(updateOriginChannelUseCase).execute(
                argThat(command -> command.originChannelId().value().equals(8L)
                        && "Llamada institucional".equals(command.name())),
                any()
        );
    }

    @Test
    void catalogControllerMustExposeOnlyBatch4ARequestTypeAndOriginChannelMappings() {
        var mappings = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().equals(CatalogController.class))
                .map(entry -> describeMapping(entry.getKey()))
                .sorted(Comparator.naturalOrder())
                .toList();

        assertThat(mappings).containsExactly(
                "GET /api/v1/catalogs/origin-channels",
                "GET /api/v1/catalogs/origin-channels/{channelId}",
                "GET /api/v1/catalogs/request-types",
                "GET /api/v1/catalogs/request-types/{typeId}",
                "POST /api/v1/catalogs/origin-channels",
                "POST /api/v1/catalogs/request-types",
                "PUT /api/v1/catalogs/origin-channels/{channelId}",
                "PUT /api/v1/catalogs/request-types/{typeId}"
        );
    }

    @Test
    void createThenGetRequestTypeMustRemainConsistentAtHttpContractLevel() throws Exception {
        given(createRequestTypeUseCase.execute(any(), any())).willReturn(
                new RequestType(new RequestTypeId(34L), "Validación", "Solicitud nueva", true)
        );
        given(getRequestTypeQuery.execute(any(), any())).willReturn(
                new RequestType(new RequestTypeId(34L), "Validación", "Solicitud nueva", true)
        );

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/catalogs/request-types")
                        .with(adminAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Validación",
                                  "description": "Solicitud nueva"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/catalogs/request-types/34"))
                .andExpect(jsonPath("$.id").value(34));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/catalogs/request-types/{typeId}", 34L)
                        .with(adminAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(34))
                .andExpect(jsonPath("$.name").value("Validación"))
                .andExpect(jsonPath("$.description").value("Solicitud nueva"))
                .andExpect(jsonPath("$.active").value(true));
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
        CatalogRestMapper catalogRestMapper() {
            return new CatalogRestMapper();
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

    private static String describeMapping(RequestMappingInfo mappingInfo) {
        var methods = mappingInfo.getMethodsCondition().getMethods().stream()
                .map(Enum::name)
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("ANY");
        var paths = mappingInfo.getPatternValues().stream()
                .sorted()
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return methods + " " + paths;
    }
}
