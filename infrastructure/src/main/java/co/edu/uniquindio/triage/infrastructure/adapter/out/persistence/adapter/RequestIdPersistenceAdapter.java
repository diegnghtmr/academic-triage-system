package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.out.persistence.NextRequestIdPort;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
class RequestIdPersistenceAdapter implements NextRequestIdPort {

    private final RequestJpaRepository requestJpaRepository;

    RequestIdPersistenceAdapter(RequestJpaRepository requestJpaRepository) {
        this.requestJpaRepository = Objects.requireNonNull(requestJpaRepository);
    }

    @Override
    public RequestId nextId() {
        var nextValue = requestJpaRepository.findNextAutoIncrementValue()
                .filter(value -> value > 0)
                .orElseThrow(() -> new IllegalStateException("No se pudo reservar el próximo id de solicitud"));
        return new RequestId(nextValue);
    }
}
