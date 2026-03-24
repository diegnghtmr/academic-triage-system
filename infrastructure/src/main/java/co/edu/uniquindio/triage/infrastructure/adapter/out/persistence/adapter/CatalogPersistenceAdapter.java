package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.CatalogPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.OriginChannelJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestTypeJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
class CatalogPersistenceAdapter implements LoadRequestTypePort, LoadOriginChannelPort {

    private final RequestTypeJpaRepository requestTypeJpaRepository;
    private final OriginChannelJpaRepository originChannelJpaRepository;
    private final CatalogPersistenceMapper catalogPersistenceMapper;

    CatalogPersistenceAdapter(RequestTypeJpaRepository requestTypeJpaRepository,
                              OriginChannelJpaRepository originChannelJpaRepository,
                              CatalogPersistenceMapper catalogPersistenceMapper) {
        this.requestTypeJpaRepository = Objects.requireNonNull(requestTypeJpaRepository);
        this.originChannelJpaRepository = Objects.requireNonNull(originChannelJpaRepository);
        this.catalogPersistenceMapper = Objects.requireNonNull(catalogPersistenceMapper);
    }

    @Override
    public Optional<RequestType> loadById(RequestTypeId requestTypeId) {
        return requestTypeJpaRepository.findById(requestTypeId.value()).map(catalogPersistenceMapper::toDomain);
    }

    @Override
    public Optional<OriginChannel> loadById(OriginChannelId originChannelId) {
        return originChannelJpaRepository.findById(originChannelId.value()).map(catalogPersistenceMapper::toDomain);
    }
}
