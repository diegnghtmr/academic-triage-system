package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper;

import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.PasswordHash;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.UserJpaEntity;

public class UserPersistenceMapper {

    public User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                new UserId(entity.getId()),
                new Username(entity.getUsername()),
                entity.getFirstName(),
                entity.getLastName(),
                new PasswordHash(entity.getPasswordHash()),
                new Identification(entity.getIdentification()),
                new Email(entity.getEmail()),
                Role.valueOf(entity.getRole()),
                entity.isActive()
        );
    }

    public UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.getId().map(id -> id.value()).orElse(null),
                user.getUsername().value(),
                user.getEmail().value(),
                user.getIdentification().value(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.isActive(),
                user.getPasswordHash().value(),
                null
        );
    }
}
