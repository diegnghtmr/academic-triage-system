package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.AiRequestSummaryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiRequestSummaryJpaRepository extends JpaRepository<AiRequestSummaryJpaEntity, Long> {
    Optional<AiRequestSummaryJpaEntity> findByRequestIdAndRequestVersion(Long requestId, long requestVersion);
}
