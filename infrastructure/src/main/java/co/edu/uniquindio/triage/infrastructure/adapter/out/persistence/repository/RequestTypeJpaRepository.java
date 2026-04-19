package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequestTypeJpaRepository extends JpaRepository<RequestTypeJpaEntity, Long> {

    List<RequestTypeJpaEntity> findAllByOrderByNameAsc();

    List<RequestTypeJpaEntity> findAllByActiveOrderByNameAsc(boolean active);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select requestType from RequestTypeJpaEntity requestType where requestType.id = :requestTypeId")
    Optional<RequestTypeJpaEntity> findByIdForUpdate(@Param("requestTypeId") Long requestTypeId);
}
