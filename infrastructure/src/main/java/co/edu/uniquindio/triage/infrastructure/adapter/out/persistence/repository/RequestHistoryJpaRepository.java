package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestHistoryJpaRepository extends JpaRepository<RequestHistoryJpaEntity, Long> {

    /**
     * Finds all history entries for a given request ID, ordered by timestamp in descending order.
     *
     * @param requestId The ID of the academic request.
     * @return A list of history entries.
     */
    List<RequestHistoryJpaEntity> findByRequestIdOrderByTimestampDesc(Long requestId);
}
