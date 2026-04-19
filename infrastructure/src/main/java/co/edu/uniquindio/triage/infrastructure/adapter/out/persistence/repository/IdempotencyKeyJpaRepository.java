package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.IdempotencyKeyJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyJpaEntity, Long> {

    Optional<IdempotencyKeyJpaEntity> findByScopeAndPrincipalScopeAndIdempotencyKey(
            String scope, String principalScope, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select k from IdempotencyKeyJpaEntity k
            where k.scope = :scope
              and k.principalScope = :principalScope
              and k.idempotencyKey = :idempotencyKey
            """)
    Optional<IdempotencyKeyJpaEntity> findByScopeAndPrincipalScopeAndIdempotencyKeyForUpdate(
            @Param("scope") String scope,
            @Param("principalScope") String principalScope,
            @Param("idempotencyKey") String idempotencyKey);
}
