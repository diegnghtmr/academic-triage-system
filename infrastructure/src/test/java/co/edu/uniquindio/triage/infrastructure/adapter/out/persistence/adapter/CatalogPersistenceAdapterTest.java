package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.OriginChannelJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.CatalogPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.OriginChannelJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestTypeJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = CatalogPersistenceAdapterTest.TestApplication.class)
@Import(co.edu.uniquindio.triage.infrastructure.config.PersistenceConfiguration.class)
class CatalogPersistenceAdapterTest {

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
    private RequestTypeJpaRepository requestTypeJpaRepository;

    @Autowired
    private OriginChannelJpaRepository originChannelJpaRepository;

    private CatalogPersistenceAdapter catalogPersistenceAdapter;

    @BeforeEach
    void setUp() {
        originChannelJpaRepository.deleteAll();
        requestTypeJpaRepository.deleteAll();
        CatalogPersistenceMapper mapper = Mappers.getMapper(CatalogPersistenceMapper.class);
        catalogPersistenceAdapter = new CatalogPersistenceAdapter(requestTypeJpaRepository, originChannelJpaRepository, mapper);
    }

    @Test
    void loadAllRequestTypesMustFilterActiveAndSortByName() {
        saveRequestTypeEntity("Zeta", "z", true);
        saveRequestTypeEntity("Alpha", "a", true);
        saveRequestTypeEntity("Beta", "b", false);

        var activeOnly = catalogPersistenceAdapter.loadAllRequestTypes(Optional.of(true));
        var all = catalogPersistenceAdapter.loadAllRequestTypes(Optional.empty());

        assertThat(activeOnly)
                .extracting(RequestType::getName)
                .containsExactly("Alpha", "Zeta");
        assertThat(all)
                .extracting(RequestType::getName)
                .containsExactly("Alpha", "Beta", "Zeta");
    }

    @Test
    void loadAllOriginChannelsMustFilterActiveAndSortByName() {
        saveOriginChannelEntity("WhatsApp", true);
        saveOriginChannelEntity("Correo batch4a", true);
        saveOriginChannelEntity("Banner", false);

        var activeOnly = catalogPersistenceAdapter.loadAllOriginChannels(Optional.of(true));
        var inactiveOnly = catalogPersistenceAdapter.loadAllOriginChannels(Optional.of(false));

        assertThat(activeOnly)
                .extracting(OriginChannel::getName)
                .containsExactly("Correo batch4a", "WhatsApp");
        assertThat(inactiveOnly)
                .extracting(OriginChannel::getName)
                .containsExactly("Banner");
    }

    @Test
    void saveRequestTypeMustAssignIdAndPersistUpdatesRoundTrip() {
        var created = catalogPersistenceAdapter.saveRequestType(RequestType.createNew("Homologación batch4a", "Inicial"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getDescription()).isEqualTo("Inicial");
        assertThat(created.isActive()).isTrue();

        created.updateName("Homologación externa");
        created.updateDescription("Actualizada");
        created.deactivate();

        var updated = catalogPersistenceAdapter.saveRequestType(created);
        var reloaded = catalogPersistenceAdapter.loadById(updated.getId());

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.orElseThrow().getName()).isEqualTo("Homologación externa");
        assertThat(reloaded.orElseThrow().getDescription()).isEqualTo("Actualizada");
        assertThat(reloaded.orElseThrow().isActive()).isFalse();
    }

    @Test
    void saveOriginChannelMustAssignIdAndPersistUpdatesRoundTrip() {
        var created = catalogPersistenceAdapter.saveOriginChannel(OriginChannel.createNew("Portal web"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.isActive()).isTrue();

        created.updateName("Portal estudiante");
        created.deactivate();

        var updated = catalogPersistenceAdapter.saveOriginChannel(created);
        var reloaded = catalogPersistenceAdapter.loadById(updated.getId());

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.orElseThrow().getName()).isEqualTo("Portal estudiante");
        assertThat(reloaded.orElseThrow().isActive()).isFalse();
    }

    @Test
    void saveRequestTypeMustTranslateUniqueConstraintViolations() {
        catalogPersistenceAdapter.saveRequestType(RequestType.createNew("Certificados batch4a", "Base"));

        assertThatThrownBy(() -> catalogPersistenceAdapter.saveRequestType(
                RequestType.createNew("Certificados batch4a", "Duplicado")
        )).isInstanceOf(DuplicateCatalogEntryException.class)
                .hasMessageContaining("tipo de solicitud")
                .hasMessageContaining("Certificados batch4a");
    }

    @Test
    void existsRequestTypeByNameIgnoreCaseAndIdNotMustExcludeCurrentRecord() {
        var saved = catalogPersistenceAdapter.saveRequestType(RequestType.createNew("Tipo guard batch4a", "Base"));

        assertThat(catalogPersistenceAdapter.existsRequestTypeByNameIgnoreCase("tipo guard batch4a")).isTrue();
        assertThat(catalogPersistenceAdapter.existsRequestTypeByNameIgnoreCaseAndIdNot("tipo guard batch4a", saved.getId())).isFalse();

        var other = catalogPersistenceAdapter.saveRequestType(RequestType.createNew("Otro tipo guard batch4a", "Otra"));

        assertThat(catalogPersistenceAdapter.existsRequestTypeByNameIgnoreCaseAndIdNot("tipo guard batch4a", other.getId())).isTrue();
    }

    @Test
    void saveOriginChannelMustTranslateUniqueConstraintViolations() {
        catalogPersistenceAdapter.saveOriginChannel(OriginChannel.createNew("Correo duplicado batch4a"));

        assertThatThrownBy(() -> catalogPersistenceAdapter.saveOriginChannel(
                OriginChannel.createNew("Correo duplicado batch4a")
        )).isInstanceOf(DuplicateCatalogEntryException.class)
                .hasMessageContaining("canal de origen")
                .hasMessageContaining("Correo duplicado batch4a");
    }

    @Test
    void existsOriginChannelByNameIgnoreCaseAndIdNotMustExcludeCurrentRecord() {
        var saved = catalogPersistenceAdapter.saveOriginChannel(OriginChannel.createNew("Canal guard batch4a"));

        assertThat(catalogPersistenceAdapter.existsOriginChannelByNameIgnoreCase("canal guard batch4a")).isTrue();
        assertThat(catalogPersistenceAdapter.existsOriginChannelByNameIgnoreCaseAndIdNot("canal guard batch4a", saved.getId())).isFalse();

        var other = catalogPersistenceAdapter.saveOriginChannel(OriginChannel.createNew("Otro canal guard batch4a"));

        assertThat(catalogPersistenceAdapter.existsOriginChannelByNameIgnoreCaseAndIdNot("canal guard batch4a", other.getId())).isTrue();
    }

    @Test
    void loadByIdMustRemainCompatibleForExistingLifecycleReads() {
        var requestTypeEntity = saveRequestTypeEntity("Reintegro compat batch4a", "Lectura legacy", true);
        var originChannelEntity = saveOriginChannelEntity("Ventanilla compat batch4a", true);

        var loadedRequestType = catalogPersistenceAdapter.loadById(new RequestTypeId(requestTypeEntity.getId()));
        var loadedOriginChannel = catalogPersistenceAdapter.loadById(new OriginChannelId(originChannelEntity.getId()));

        assertThat(loadedRequestType).isPresent();
        assertThat(loadedRequestType.orElseThrow().getName()).isEqualTo("Reintegro compat batch4a");
        assertThat(loadedRequestType.orElseThrow().getDescription()).isEqualTo("Lectura legacy");
        assertThat(loadedRequestType.orElseThrow().isActive()).isTrue();

        assertThat(loadedOriginChannel).isPresent();
        assertThat(loadedOriginChannel.orElseThrow().getName()).isEqualTo("Ventanilla compat batch4a");
        assertThat(loadedOriginChannel.orElseThrow().isActive()).isTrue();
    }

    private RequestTypeJpaEntity saveRequestTypeEntity(String name, String description, boolean active) {
        var entity = new RequestTypeJpaEntity();
        entity.setName(name);
        entity.setDescription(description);
        entity.setActive(active);
        return requestTypeJpaRepository.saveAndFlush(entity);
    }

    private OriginChannelJpaEntity saveOriginChannelEntity(String name, boolean active) {
        var entity = new OriginChannelJpaEntity();
        entity.setName(name);
        entity.setActive(active);
        return originChannelJpaRepository.saveAndFlush(entity);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    static class TestApplication {
    }
}
