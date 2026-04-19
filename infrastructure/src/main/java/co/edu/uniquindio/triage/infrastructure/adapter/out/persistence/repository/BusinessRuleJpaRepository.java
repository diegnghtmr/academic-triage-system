package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.BusinessRuleJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessRuleJpaRepository extends JpaRepository<BusinessRuleJpaEntity, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<BusinessRuleJpaEntity> findAllByActive(Boolean active);

    List<BusinessRuleJpaEntity> findAllByConditionType(String conditionType);

    List<BusinessRuleJpaEntity> findAllByActiveAndConditionType(Boolean active, String conditionType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select businessRule from BusinessRuleJpaEntity businessRule where businessRule.id = :businessRuleId")
    Optional<BusinessRuleJpaEntity> findByIdForUpdate(@Param("businessRuleId") Long businessRuleId);
}
