package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestTypeJpaRepository extends JpaRepository<RequestTypeJpaEntity, Long> {
}
