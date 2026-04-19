package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.BusinessRuleJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.BusinessRulePersistenceMapperImpl;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.BusinessRuleJpaRepository;
import co.edu.uniquindio.triage.infrastructure.config.PersistenceConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = BusinessRuleOptimisticLockingIntegrationTest.TestApplication.class)
@Import(PersistenceConfiguration.class)
class BusinessRuleOptimisticLockingIntegrationTest {

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

    @Autowired
    private TestEntityManager testEntityManager;

    private BusinessRulePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new BusinessRulePersistenceAdapter(
                businessRuleJpaRepository,
                new BusinessRulePersistenceMapperImpl()
        );
    }

    @Test
    void versionMustBeInitializedOnFirstSave() {
        var rule = BusinessRule.createNew("Version Init Rule", "Desc", ConditionType.DEADLINE, "5", Priority.HIGH, null);
        var saved = adapter.save(rule);

        assertThat(saved.getId()).isNotNull();
        var version = adapter.findVersionById(saved.getId());
        assertThat(version).isPresent();
        assertThat(version.get()).isNotNull();
    }

    @Test
    void versionMustIncrementAfterUpdate() {
        var entity = testEntityManager.persistAndFlush(buildEntity("Version Bump Rule"));
        Long id = entity.getId();
        Long versionBefore = entity.getVersion();

        var loaded = businessRuleJpaRepository.findById(id).orElseThrow();
        loaded.setDescription("Updated for version bump");
        businessRuleJpaRepository.saveAndFlush(loaded);
        testEntityManager.detach(loaded);

        Long versionAfter = businessRuleJpaRepository.findById(id)
                .map(BusinessRuleJpaEntity::getVersion)
                .orElseThrow();

        assertThat(versionAfter).isGreaterThan(versionBefore);
    }

    @Test
    void concurrentUpdateWithStaleVersionMustThrow() {
        // 1. Create and flush an entity — version starts at 0
        var persisted = testEntityManager.persistAndFlush(buildEntity("Concurrent Locking Rule"));
        Long id = persisted.getId();
        assertThat(persisted.getVersion()).isNotNull();

        // 2. Detach to simulate a stale reference held by another session
        testEntityManager.detach(persisted);

        // 3. A second "session" loads and updates the entity — version bumps
        var fresh = businessRuleJpaRepository.findById(id).orElseThrow();
        fresh.setDescription("Updated by concurrent session");
        businessRuleJpaRepository.saveAndFlush(fresh); // version is now staleVersion + 1
        testEntityManager.detach(fresh);

        // 4. The stale reference attempts an update — must be rejected
        persisted.setDescription("Stale update attempt");
        assertThatThrownBy(() -> businessRuleJpaRepository.saveAndFlush(persisted))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void findVersionByIdMustReturnCurrentDatabaseVersion() {
        var entity = testEntityManager.persistAndFlush(buildEntity("Version Query Rule"));
        Long id = entity.getId();

        var version = adapter.findVersionById(new BusinessRuleId(id));

        assertThat(version).isPresent();
        assertThat(version.get()).isEqualTo(entity.getVersion());
    }

    private BusinessRuleJpaEntity buildEntity(String name) {
        return BusinessRuleJpaEntity.builder()
                .name(name)
                .description("Integration test entity")
                .conditionType(ConditionType.DEADLINE.name())
                .conditionValue("7")
                .resultingPriority(Priority.HIGH.name())
                .active(true)
                .build();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
