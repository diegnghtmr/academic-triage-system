package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.in.ai.AiGeneratedSummary;
import co.edu.uniquindio.triage.application.port.out.ai.LoadAiSummaryCachePort;
import co.edu.uniquindio.triage.application.port.out.ai.SaveAiSummaryCachePort;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.AiRequestSummaryJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.AiRequestSummaryJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
class AiSummaryCachePersistenceAdapter implements LoadAiSummaryCachePort, SaveAiSummaryCachePort {

    private final AiRequestSummaryJpaRepository repository;

    AiSummaryCachePersistenceAdapter(AiRequestSummaryJpaRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Optional<AiGeneratedSummary> findByRequestIdAndVersion(RequestId requestId, long requestVersion) {
        return repository.findByRequestIdAndRequestVersion(requestId.value(), requestVersion)
                .map(e -> new AiGeneratedSummary(requestId, e.getSummary(), e.getGeneratedAt()));
    }

    @Override
    public void save(RequestId requestId, long requestVersion, AiGeneratedSummary summary) {
        var entity = AiRequestSummaryJpaEntity.builder()
                .requestId(requestId.value())
                .requestVersion(requestVersion)
                .summary(summary.summary())
                .generatedAt(summary.generatedAt())
                .build();
        repository.save(entity);
    }
}
