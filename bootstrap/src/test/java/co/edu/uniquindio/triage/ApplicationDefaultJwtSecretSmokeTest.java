package co.edu.uniquindio.triage;

import co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration;
import co.edu.uniquindio.triage.support.MinimalLoadUserAuthPortTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class ApplicationDefaultJwtSecretSmokeTest {

    @Test
    void startupMustFailWithoutJwtSecretOutsideDevAndTestProfiles() {
        assertThatThrownBy(() -> runApplication())
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .rootCause()
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void devProfileMustProvideLocalOnlyJwtFallback() {
        assertThatCode(() -> {
            try (var ignored = runApplication("--spring.profiles.active=dev")) {
                // Context startup is the regression boundary: dev may boot locally without an external JWT secret.
            }
        }).doesNotThrowAnyException();
    }

    private ConfigurableApplicationContext runApplication(String... args) {
        var application = new SpringApplication(TestApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setEnvironment(isolatedEnvironment());
        return application.run(args);
    }

    private StandardEnvironment isolatedEnvironment() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);

        var properties = new HashMap<String, Object>();
        properties.put("app.jwt.expiration-ms", "86400000");
        environment.getPropertySources().addFirst(new MapPropertySource("isolated-test-overrides", properties));
        return environment;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import({SecurityConfiguration.class, MinimalLoadUserAuthPortTestConfiguration.class})
    static class TestApplication {
    }
}
