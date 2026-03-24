package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.OriginChannelJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OriginChannelJpaRepository extends JpaRepository<OriginChannelJpaEntity, Long> {
}
