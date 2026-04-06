package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestHistoryJpaRepository extends JpaRepository<RequestHistoryJpaEntity, Long> {

    /**
     * Finds all history entries for a given request ID, ordered by timestamp in descending order.
     *
     * @param requestId The ID of the academic request.
     * @return A list of history entries.
     */
    List<RequestHistoryJpaEntity> findByRequestIdOrderByTimestampDesc(Long requestId);

    @Query(value = """
            SELECT COALESCE(AVG(TIMESTAMPDIFF(SECOND, r.registration_date, h.timestamp)) / 3600.0, 0.0)
            FROM academic_requests r
            JOIN request_history h ON r.id = h.request_id
            WHERE h.action = 'CLOSED'
            AND (:from IS NULL OR h.timestamp >= :from)
            AND (:to IS NULL OR h.timestamp < :to)
            """, nativeQuery = true)
    Double calculateAverageResolutionTimeHours(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);

    @Query(value = """
            SELECT COALESCE(h.responsible_id, h.performed_by_id) as resolved_responsible_id, COUNT(h.id) as closed_count
            FROM request_history h
            WHERE h.action = 'CLOSED'
            AND (:from IS NULL OR h.timestamp >= :from)
            AND (:to IS NULL OR h.timestamp < :to)
            GROUP BY resolved_responsible_id
            ORDER BY closed_count DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> findTopResponsiblesByClosedRequests(@Param("from") java.time.LocalDateTime from, @Param("to") java.time.LocalDateTime to);
}
