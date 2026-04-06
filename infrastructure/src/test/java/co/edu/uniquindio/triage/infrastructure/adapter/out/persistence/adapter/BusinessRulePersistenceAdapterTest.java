package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.BusinessRuleJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.BusinessRulePersistenceMapperImpl;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.BusinessRuleJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = BusinessRulePersistenceAdapterTest.TestApplication.class)
@Import(co.edu.uniquindio.triage.infrastructure.config.PersistenceConfiguration.class)
class BusinessRulePersistenceAdapterTest {

    private static final MariaDBContainer<?> MARIADB = new MariaDBContainer<>("mariadb:11");

    static {
        MARIADB.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MARIADB::getJdbcUrl);
        registry.add("spring.datasource.username", MARIADB::getUsername);
        registry.add("spring.datasource.password", MARIADB::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
    }

    @Autowired
    private BusinessRuleJpaRepository businessRuleJpaRepository;

    private BusinessRulePersistenceAdapter businessRulePersistenceAdapter;

    @BeforeEach
    void setUp() {
        businessRulePersistenceAdapter = new BusinessRulePersistenceAdapter(
                businessRuleJpaRepository,
                new BusinessRulePersistenceMapperImpl()
        );
    }

    @Test
    void saveAndLoadMustPreserveCanonicalMapping() {
        var rule = BusinessRule.createNew(
                "Regla de Prueba",
                "Descripción de prueba",
                ConditionType.DEADLINE,
                "{\"days\": 5}",
                Priority.HIGH,
                null
        );

        var saved = businessRulePersistenceAdapter.save(rule);

        assertThat(saved.getId()).isNotNull();
        var loaded = businessRulePersistenceAdapter.findById(saved.getId());

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getName()).isEqualTo("Regla de Prueba");
        assertThat(loaded.get().getDescription()).isEqualTo("Descripción de prueba");
        assertThat(loaded.get().getConditionType()).isEqualTo(ConditionType.DEADLINE);
        assertThat(loaded.get().getConditionValue()).isEqualTo("{\"days\": 5}");
        assertThat(loaded.get().getResultingPriority()).isEqualTo(Priority.HIGH);
        assertThat(loaded.get().isActive()).isTrue();
    }

    @Test
    void findAllByActiveAndConditionTypeShouldFilterCorrectly() {
        businessRuleJpaRepository.saveAndFlush(entity("Rule 1", true, ConditionType.DEADLINE));
        businessRuleJpaRepository.saveAndFlush(entity("Rule 2", false, ConditionType.DEADLINE));
        businessRuleJpaRepository.saveAndFlush(entity("Rule 3", true, ConditionType.IMPACT_LEVEL));

        List<BusinessRule> activeDeadlineRules = businessRulePersistenceAdapter.findAll(true, ConditionType.DEADLINE)
                .stream().filter(r -> r.getName().startsWith("Rule ")).toList();
        assertThat(activeDeadlineRules).hasSize(1);
        assertThat(activeDeadlineRules.get(0).getName()).isEqualTo("Rule 1");

        List<BusinessRule> allActiveRules = businessRulePersistenceAdapter.findAll(true, null)
                .stream().filter(r -> r.getName().startsWith("Rule ")).toList();
        assertThat(allActiveRules).hasSize(2);

        List<BusinessRule> allDeadlineRules = businessRulePersistenceAdapter.findAll(null, ConditionType.DEADLINE)
                .stream().filter(r -> r.getName().startsWith("Rule ")).toList();
        assertThat(allDeadlineRules).hasSize(2);
    }

    @Test
    void existsByNameShouldBeCaseInsensitive() {
        businessRuleJpaRepository.save(entity("Unique Rule", true, ConditionType.DEADLINE));

        assertThat(businessRulePersistenceAdapter.existsByName("Unique Rule")).isTrue();
        assertThat(businessRulePersistenceAdapter.existsByName("unique rule")).isTrue();
        assertThat(businessRulePersistenceAdapter.existsByName("NON EXISTENT")).isFalse();
    }

    @Test
    void existsByNameAndIdNotShouldExcludeCurrentId() {
        var saved = businessRuleJpaRepository.save(entity("Existing Rule", true, ConditionType.DEADLINE));
        BusinessRuleId ruleId = new BusinessRuleId(saved.getId());

        assertThat(businessRulePersistenceAdapter.existsByNameAndIdNot("Existing Rule", ruleId)).isFalse();
        assertThat(businessRulePersistenceAdapter.existsByNameAndIdNot("existing rule", ruleId)).isFalse();

        businessRuleJpaRepository.save(entity("Other Rule", true, ConditionType.DEADLINE));
        assertThat(businessRulePersistenceAdapter.existsByNameAndIdNot("Other Rule", ruleId)).isTrue();
    }

    @Test
    void repositoryMustRejectDuplicateName() {
        businessRuleJpaRepository.saveAndFlush(entity("Duplicate Name", true, ConditionType.DEADLINE));

        assertThatThrownBy(() -> businessRuleJpaRepository.saveAndFlush(
                entity("Duplicate Name", false, ConditionType.IMPACT_LEVEL)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private BusinessRuleJpaEntity entity(String name, boolean active, ConditionType conditionType) {
        return BusinessRuleJpaEntity.builder()
                .name(name)
                .description("Desc")
                .conditionType(conditionType.name())
                .conditionValue("{}")
                .resultingPriority(Priority.MEDIUM.name())
                .active(active)
                .build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
