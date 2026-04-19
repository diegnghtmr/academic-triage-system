package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.AcademicRequestJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface RequestJpaRepository extends JpaRepository<AcademicRequestJpaEntity, Long>, JpaSpecificationExecutor<AcademicRequestJpaEntity> {

    @Override
    @EntityGraph(attributePaths = {"applicant", "responsible", "originChannel", "requestType"})
    Page<AcademicRequestJpaEntity> findAll(org.springframework.data.jpa.domain.Specification<AcademicRequestJpaEntity> spec,
                                           Pageable pageable);

    @Query("""
            select distinct request
            from AcademicRequestJpaEntity request
            left join fetch request.applicant
            left join fetch request.responsible
            left join fetch request.originChannel
            left join fetch request.requestType
            left join fetch request.history historyEntry
            left join fetch historyEntry.performedBy
            left join fetch historyEntry.responsible
            where request.id = :requestId
            """)
    Optional<AcademicRequestJpaEntity> findDetailedById(@Param("requestId") Long requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from AcademicRequestJpaEntity request where request.id = :requestId")
    Optional<AcademicRequestJpaEntity> findByIdForUpdate(@Param("requestId") Long requestId);

    @Query(value = """
            select coalesce(auto_increment, 1)
            from information_schema.tables
            where table_schema = database()
              and table_name = 'academic_requests'
            """, nativeQuery = true)
    Optional<Long> findNextAutoIncrementValue();

    @Query("SELECT COUNT(r) FROM AcademicRequestJpaEntity r WHERE (:from IS NULL OR r.registrationDateTime >= :from) AND (:to IS NULL OR r.registrationDateTime < :to)")
    long countByRegistrationDateTime(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT r.status, COUNT(r) FROM AcademicRequestJpaEntity r WHERE (:from IS NULL OR r.registrationDateTime >= :from) AND (:to IS NULL OR r.registrationDateTime < :to) GROUP BY r.status")
    java.util.List<Object[]> countByStatusAndRegistrationDateTime(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT rt.name, COUNT(r) FROM AcademicRequestJpaEntity r JOIN r.requestType rt WHERE (:from IS NULL OR r.registrationDateTime >= :from) AND (:to IS NULL OR r.registrationDateTime < :to) GROUP BY rt.name")
    java.util.List<Object[]> countByTypeNameAndRegistrationDateTime(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query("SELECT r.priority, COUNT(r) FROM AcademicRequestJpaEntity r WHERE (:from IS NULL OR r.registrationDateTime >= :from) AND (:to IS NULL OR r.registrationDateTime < :to) GROUP BY r.priority")
    java.util.List<Object[]> countByPriorityAndRegistrationDateTime(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);
}
