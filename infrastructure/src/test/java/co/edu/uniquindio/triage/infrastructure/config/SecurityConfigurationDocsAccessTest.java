package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.infrastructure.adapter.out.security.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
        classes = SecurityConfigurationDocsDisabledByDefaultTest.TestApplication.class,
        properties = {
                "app.jwt.secret=12345678901234567890123456789012",
                "app.jwt.expiration-ms=86400000",
                "spring.ai.openai.api-key=test-key",
                "springdoc.api-docs.path=/api-docs",
                "springdoc.swagger-ui.path=/swagger-ui.html",
                "app.docs.enabled=false",
                "app.docs.public-enabled=false"
        }
)
@AutoConfigureMockMvc
class SecurityConfigurationDocsDisabledByDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthMustRemainPublic() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
    }

    @Test
    void docsMustBeHiddenFromAnonymousCallersWhenDisabledByDefault() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api-docs"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        mockMvc.perform(MockMvcRequestBuilders.get("/swagger-ui.html"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import({SecurityConfiguration.class, HealthProbeController.class})
    static class TestApplication {
    }

    @RestController
    static class HealthProbeController {

        @GetMapping("/actuator/health")
        java.util.Map<String, String> health() {
            return java.util.Map.of("status", "UP");
        }
    }
}

@SpringBootTest(
        classes = SecurityConfigurationDocsPublicAccessTest.TestApplication.class,
        properties = {
                "app.jwt.secret=12345678901234567890123456789012",
                "app.jwt.expiration-ms=86400000",
                "spring.ai.openai.api-key=test-key",
                "springdoc.api-docs.path=/api-docs",
                "springdoc.swagger-ui.path=/swagger-ui.html",
                "app.docs.enabled=true",
                "app.docs.public-enabled=true"
        }
)
@AutoConfigureMockMvc
class SecurityConfigurationDocsPublicAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void docsMustBePublicWhenDocsModeIsExplicitlyEnabledForLocalStyleUse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api-docs"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.get("/swagger-ui.html"))
                .andExpect(MockMvcResultMatchers.status().is3xxRedirection());
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import({SecurityConfiguration.class, SecurityConfigurationDocsDisabledByDefaultTest.HealthProbeController.class})
    static class TestApplication {
    }
}

@SpringBootTest(
        classes = SecurityConfigurationDocsRestrictedOptInTest.TestApplication.class,
        properties = {
                "app.jwt.secret=12345678901234567890123456789012",
                "app.jwt.expiration-ms=86400000",
                "spring.ai.openai.api-key=test-key",
                "springdoc.api-docs.path=/api-docs",
                "springdoc.swagger-ui.path=/swagger-ui.html",
                "app.docs.enabled=true",
                "app.docs.public-enabled=false"
        }
)
@AutoConfigureMockMvc
class SecurityConfigurationDocsRestrictedOptInTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void docsMustRequireAdminWhenEnabledOutsidePublicDocsModes() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api-docs"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        mockMvc.perform(MockMvcRequestBuilders.get("/api-docs").with(adminAuthentication()))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    private RequestPostProcessor adminAuthentication() {
        var principal = new AuthenticatedUser(99L, "admin", Role.ADMIN, true);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import({SecurityConfiguration.class, SecurityConfigurationDocsDisabledByDefaultTest.HealthProbeController.class})
    static class TestApplication {
    }
}
