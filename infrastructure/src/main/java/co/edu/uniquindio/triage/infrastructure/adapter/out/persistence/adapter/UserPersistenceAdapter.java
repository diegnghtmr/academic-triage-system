package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveUserPort;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.UserJpaRepository;

import java.util.Objects;
import java.util.Optional;

public class UserPersistenceAdapter implements LoadUserAuthPort, SaveUserPort {

    private final UserJpaRepository userJpaRepository;
    private final UserPersistenceMapper userPersistenceMapper;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository,
                                  UserPersistenceMapper userPersistenceMapper) {
        this.userJpaRepository = Objects.requireNonNull(userJpaRepository);
        this.userPersistenceMapper = Objects.requireNonNull(userPersistenceMapper);
    }

    @Override
    public Optional<User> loadByUsername(Username username) {
        return userJpaRepository.findByUsername(username.value()).map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> loadByEmail(Email email) {
        return userJpaRepository.findByEmail(email.value()).map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> loadById(UserId id) {
        return userJpaRepository.findById(id.value()).map(userPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(Username username) {
        return userJpaRepository.existsByUsername(username.value());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return userJpaRepository.existsByEmail(email.value());
    }

    @Override
    public boolean existsByIdentification(Identification identification) {
        return userJpaRepository.existsByIdentification(identification.value());
    }

    @Override
    public User save(User user) {
        var entity = userPersistenceMapper.toEntity(user);
        return userPersistenceMapper.toDomain(userJpaRepository.save(entity));
    }
}
