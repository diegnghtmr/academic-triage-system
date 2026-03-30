package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.in.common.Page;
import co.edu.uniquindio.triage.application.port.in.user.command.GetUsersQueryModel;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUserAuthPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadUsersPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveUserPort;
import co.edu.uniquindio.triage.domain.model.Email;
import co.edu.uniquindio.triage.domain.model.Identification;
import co.edu.uniquindio.triage.domain.model.User;
import co.edu.uniquindio.triage.domain.model.Username;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.specification.UserJpaSpecification;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class UserPersistenceAdapter implements LoadUserAuthPort, SaveUserPort, LoadUsersPort {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "username", "username",
            "email", "email",
            "firstName", "firstName",
            "lastName", "lastName",
            "role", "role",
            "active", "active"
    );

    private final UserJpaRepository userJpaRepository;
    private final UserPersistenceMapper userPersistenceMapper;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository,
                                  UserPersistenceMapper userPersistenceMapper) {
        this.userJpaRepository = Objects.requireNonNull(userJpaRepository);
        this.userPersistenceMapper = Objects.requireNonNull(userPersistenceMapper);
    }

    @Override
    public Page<User> loadAll(GetUsersQueryModel query) {
        var page = userJpaRepository.findAll(
                UserJpaSpecification.withModel(query),
                PageRequest.of(query.page(), query.size(), toSort(query.sort()))
        );

        var content = page.getContent().stream()
                .map(userPersistenceMapper::toDomain)
                .toList();

        return new Page<>(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    private Sort toSort(String sortExpression) {
        var segments = sortExpression.split(",", 2);
        if (segments.length != 2) {
            throw new IllegalArgumentException("El sort debe incluir campo y dirección separados por coma");
        }

        var requestedField = segments[0].trim();
        var directionToken = segments[1].trim();
        var property = SORT_FIELDS.get(requestedField);
        if (property == null) {
            throw new IllegalArgumentException("Campo de ordenamiento no soportado: " + requestedField);
        }

        var direction = Sort.Direction.fromOptionalString(directionToken)
                .orElseThrow(() -> new IllegalArgumentException("Dirección de sort inválida: " + directionToken));

        return Sort.by(direction, property);
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
