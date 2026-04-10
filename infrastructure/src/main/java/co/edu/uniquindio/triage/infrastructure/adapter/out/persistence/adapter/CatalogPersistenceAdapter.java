package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.out.persistence.LoadOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveOriginChannelPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestTypePort;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.CatalogPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.OriginChannelJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestTypeJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.support.MariaDbUniqueViolation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
class CatalogPersistenceAdapter implements LoadRequestTypePort, SaveRequestTypePort, LoadOriginChannelPort, SaveOriginChannelPort {

    private static final String REQUEST_TYPE_CATALOG_NAME = "tipo de solicitud";
    private static final String ORIGIN_CHANNEL_CATALOG_NAME = "canal de origen";

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
    public List<RequestType> loadAllRequestTypes(Optional<Boolean> active) {
        return active.map(requestTypeJpaRepository::findAllByActiveOrderByNameAsc)
                .orElseGet(requestTypeJpaRepository::findAllByOrderByNameAsc)
                .stream()
                .map(catalogPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsRequestTypeByNameIgnoreCase(String name) {
        return requestTypeJpaRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsRequestTypeByNameIgnoreCaseAndIdNot(String name, RequestTypeId requestTypeId) {
        return requestTypeJpaRepository.existsByNameIgnoreCaseAndIdNot(name, requestTypeId.value());
    }

    @Override
    public RequestType saveRequestType(RequestType requestType) {
        try {
            var saved = requestTypeJpaRepository.saveAndFlush(catalogPersistenceMapper.toEntity(requestType));
            return catalogPersistenceMapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            if (MariaDbUniqueViolation.isUniqueViolation(exception)) {
                throw new DuplicateCatalogEntryException(REQUEST_TYPE_CATALOG_NAME, requestType.getName());
            }
            throw exception;
        }
    }

    @Override
    public Optional<OriginChannel> loadById(OriginChannelId originChannelId) {
        return originChannelJpaRepository.findById(originChannelId.value()).map(catalogPersistenceMapper::toDomain);
    }

    @Override
    public List<OriginChannel> loadAllOriginChannels(Optional<Boolean> active) {
        return active.map(originChannelJpaRepository::findAllByActiveOrderByNameAsc)
                .orElseGet(originChannelJpaRepository::findAllByOrderByNameAsc)
                .stream()
                .map(catalogPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsOriginChannelByNameIgnoreCase(String name) {
        return originChannelJpaRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsOriginChannelByNameIgnoreCaseAndIdNot(String name, OriginChannelId originChannelId) {
        return originChannelJpaRepository.existsByNameIgnoreCaseAndIdNot(name, originChannelId.value());
    }

    @Override
    public OriginChannel saveOriginChannel(OriginChannel originChannel) {
        try {
            var saved = originChannelJpaRepository.saveAndFlush(catalogPersistenceMapper.toEntity(originChannel));
            return catalogPersistenceMapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            if (MariaDbUniqueViolation.isUniqueViolation(exception)) {
                throw new DuplicateCatalogEntryException(ORIGIN_CHANNEL_CATALOG_NAME, originChannel.getName());
            }
            throw exception;
        }
    }
}
