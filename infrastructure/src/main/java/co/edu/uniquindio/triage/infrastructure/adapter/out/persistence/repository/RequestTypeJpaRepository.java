package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestTypeJpaRepository extends JpaRepository<RequestTypeJpaEntity, Long> {

    List<RequestTypeJpaEntity> findAllByOrderByNameAsc();

    List<RequestTypeJpaEntity> findAllByActiveOrderByNameAsc(boolean active);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
