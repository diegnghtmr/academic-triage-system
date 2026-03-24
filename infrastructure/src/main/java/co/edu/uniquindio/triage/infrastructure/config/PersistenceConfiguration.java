package co.edu.uniquindio.triage.infrastructure.config;

import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.AuthRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.RequestRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter.UserPersistenceAdapter;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity")
@EnableJpaRepositories(basePackages = "co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository")
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

    @Bean
    UserRestMapper userRestMapper() {
        return new UserRestMapper();
    }

    @Bean
    AuthRestMapper authRestMapper(UserRestMapper userRestMapper) {
        return new AuthRestMapper(userRestMapper);
    }

    @Bean
    RequestRestMapper requestRestMapper(UserRestMapper userRestMapper) {
        return new RequestRestMapper(userRestMapper);
    }
}
