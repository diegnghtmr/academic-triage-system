package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.specification;

import co.edu.uniquindio.triage.application.port.out.persistence.UserSearchCriteria;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.domain.Specification;

public final class UserJpaSpecification {

    private UserJpaSpecification() {
    }

    public static Specification<UserJpaEntity> withCriteria(UserSearchCriteria criteria) {
        return Specification.<UserJpaEntity>where(hasRole(criteria))
                .and(hasActive(criteria));
    }

    private static Specification<UserJpaEntity> hasRole(UserSearchCriteria criteria) {
        return criteria.role()
                .map(role -> (Specification<UserJpaEntity>) (root, query, cb) -> cb.equal(root.get("role"), role.name()))
                .orElse(null);
    }

    private static Specification<UserJpaEntity> hasActive(UserSearchCriteria criteria) {
        return criteria.active()
                .map(active -> (Specification<UserJpaEntity>) (root, query, cb) -> cb.equal(root.get("active"), active))
                .orElse(null);
    }
}
