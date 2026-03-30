package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.BusinessRuleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessRuleJpaRepository extends JpaRepository<BusinessRuleJpaEntity, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<BusinessRuleJpaEntity> findAllByActive(Boolean active);

    List<BusinessRuleJpaEntity> findAllByConditionType(String conditionType);

    List<BusinessRuleJpaEntity> findAllByActiveAndConditionType(Boolean active, String conditionType);
}
