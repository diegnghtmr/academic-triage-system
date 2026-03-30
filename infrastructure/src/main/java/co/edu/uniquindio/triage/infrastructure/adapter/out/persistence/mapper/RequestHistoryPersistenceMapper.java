package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper;

import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.model.RequestHistory;
import co.edu.uniquindio.triage.domain.model.id.RequestHistoryId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestHistoryJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RequestHistoryPersistenceMapper {

    public RequestHistory toDomain(RequestHistoryJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new RequestHistory(
                entity.getId() == null ? null : new RequestHistoryId(entity.getId()),
                HistoryAction.valueOf(entity.getAction()),
                entity.getObservations(),
                entity.getTimestamp(),
                new RequestId(entity.getRequest().getId()),
                new UserId(entity.getPerformedBy().getId()),
                entity.getResponsible() == null ? null : new UserId(entity.getResponsible().getId())
        );
    }

    public List<RequestHistory> toDomainList(List<RequestHistoryJpaEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
}
