package co.edu.uniquindio.triage;

import co.edu.uniquindio.triage.infrastructure.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = ApplicationDefaultJwtSecretSmokeTest.TestApplication.class
)
class ApplicationDefaultJwtSecretSmokeTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextMustCreateTokenProviderWithDefaultJwtSecret() {
        assertThat(applicationContext.containsBean("tokenProviderPort")).isTrue();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import(SecurityConfiguration.class)
    static class TestApplication {
    }
}
