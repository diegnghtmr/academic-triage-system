package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.OriginChannelJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OriginChannelJpaRepository extends JpaRepository<OriginChannelJpaEntity, Long> {

    List<OriginChannelJpaEntity> findAllByOrderByNameAsc();

    List<OriginChannelJpaEntity> findAllByActiveOrderByNameAsc(boolean active);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
