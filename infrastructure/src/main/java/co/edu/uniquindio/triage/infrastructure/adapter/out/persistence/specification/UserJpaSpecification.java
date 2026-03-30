package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.specification;

import co.edu.uniquindio.triage.application.port.in.user.command.GetUsersQueryModel;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.domain.Specification;

public final class UserJpaSpecification {

    private UserJpaSpecification() {
    }

    public static Specification<UserJpaEntity> withModel(GetUsersQueryModel model) {
        return Specification.<UserJpaEntity>where(hasRole(model))
                .and(hasActive(model));
    }

    private static Specification<UserJpaEntity> hasRole(GetUsersQueryModel model) {
        return model.role()
                .map(role -> (Specification<UserJpaEntity>) (root, query, cb) -> cb.equal(root.get("role"), role.name()))
                .orElse(null);
    }

    private static Specification<UserJpaEntity> hasActive(GetUsersQueryModel model) {
        return model.active()
                .map(active -> (Specification<UserJpaEntity>) (root, query, cb) -> cb.equal(root.get("active"), active))
                .orElse(null);
    }
}
