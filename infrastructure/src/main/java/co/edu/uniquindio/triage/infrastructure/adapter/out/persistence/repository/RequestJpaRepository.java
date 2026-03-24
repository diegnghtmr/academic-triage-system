package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.AcademicRequestJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            where request.id = :requestId
            """)
    Optional<AcademicRequestJpaEntity> findDetailedById(@Param("requestId") Long requestId);

    @Query(value = """
            select coalesce(auto_increment, 1)
            from information_schema.tables
            where table_schema = database()
              and table_name = 'academic_requests'
            """, nativeQuery = true)
    Optional<Long> findNextAutoIncrementValue();
}
