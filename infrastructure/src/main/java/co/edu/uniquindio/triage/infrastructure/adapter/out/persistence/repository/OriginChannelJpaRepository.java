package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository;

import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.OriginChannelJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OriginChannelJpaRepository extends JpaRepository<OriginChannelJpaEntity, Long> {

    List<OriginChannelJpaEntity> findAllByOrderByNameAsc();

    List<OriginChannelJpaEntity> findAllByActiveOrderByNameAsc(boolean active);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select originChannel from OriginChannelJpaEntity originChannel where originChannel.id = :originChannelId")
    Optional<OriginChannelJpaEntity> findByIdForUpdate(@Param("originChannelId") Long originChannelId);
}
