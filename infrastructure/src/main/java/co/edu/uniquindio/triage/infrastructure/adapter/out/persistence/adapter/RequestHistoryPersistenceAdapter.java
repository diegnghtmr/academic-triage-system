package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestHistoryPort;
import co.edu.uniquindio.triage.domain.model.RequestHistory;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.RequestHistoryPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
class RequestHistoryPersistenceAdapter implements LoadRequestHistoryPort {

    private final RequestHistoryJpaRepository historyRepository;
    private final RequestHistoryPersistenceMapper mapper;

    @Override
    public List<RequestHistory> loadRequestHistory(RequestId requestId) {
        var historyEntities = historyRepository.findByRequestIdOrderByTimestampDesc(requestId.value());
        return mapper.toDomainList(historyEntities);
    }
}
