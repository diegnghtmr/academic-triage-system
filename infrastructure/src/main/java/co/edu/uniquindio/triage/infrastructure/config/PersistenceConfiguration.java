package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.AuthRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter.UserPersistenceAdapter;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity")
@EnableJpaRepositories(basePackages = "co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository")
@EnableJpaAuditing
public class PersistenceConfiguration {

    @Bean
    UserPersistenceMapper userPersistenceMapper() {
        return new UserPersistenceMapper();
    }

    @Bean
    UserPersistenceAdapter userPersistenceAdapter(UserJpaRepository userJpaRepository,
                                                  UserPersistenceMapper userPersistenceMapper) {
        return new UserPersistenceAdapter(userJpaRepository, userPersistenceMapper);
    }

    /*
     * Transitional note:
     * - CatalogPersistenceMapper and RequestRestMapper already follow the MapStruct + componentModel="spring" policy.
     * - UserPersistenceMapper, UserRestMapper, and AuthRestMapper are older hand-written mappers still instantiated directly
     *   by production code and tests outside this file.
     * - Converting them to generated MapStruct beans would require touching those mapper classes and their consumers,
     *   which is intentionally out of scope for this bounded governance fix.
     */
}

@Configuration
class LegacyRestMapperConfiguration {

    @Bean
    UserRestMapper userRestMapper() {
        return new UserRestMapper();
    }

    @Bean
    AuthRestMapper authRestMapper(UserRestMapper userRestMapper) {
        return new AuthRestMapper(userRestMapper);
    }
}
