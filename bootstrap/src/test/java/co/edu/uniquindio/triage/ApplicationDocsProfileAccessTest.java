package co.edu.uniquindio.triage;

import co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration;
import co.edu.uniquindio.triage.support.MinimalLoadUserAuthPortTestConfiguration;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ApplicationDocsDevProfileAccessTest.DocsProfileTestApplication.class,
        properties = {
                "app.jwt.secret=12345678901234567890123456789012",
                "app.jwt.expiration-ms=86400000"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ApplicationDocsDevProfileAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void docsMustBePublicWithDevProfileDefaults() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("swagger-ui")));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import({SecurityConfiguration.class, MinimalLoadUserAuthPortTestConfiguration.class})
    static class DocsProfileTestApplication {
    }
}

@SpringBootTest(
        classes = ApplicationDocsDevProfileAccessTest.DocsProfileTestApplication.class,
        properties = {
                "app.jwt.secret=12345678901234567890123456789012",
                "app.jwt.expiration-ms=86400000"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationDocsTestProfileAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void docsMustBePublicWithTestProfileDefaults() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("swagger-ui")));
    }
}

