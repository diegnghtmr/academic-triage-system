package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.in.common.Page;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.RequestSearchCriteria;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SearchRequestPort;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.AcademicRequestJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.BusinessRuleJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.OriginChannelJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.RequestPersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.specification.RequestSpecifications;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
class RequestPersistenceAdapter implements SaveRequestPort, LoadRequestPort, SearchRequestPort {

    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "registrationDateTime", "registrationDateTime",
            "status", "status",
            "priority", "priority",
            "deadline", "deadline"
    );

    private final RequestJpaRepository requestJpaRepository;
    private final RequestPersistenceMapper requestPersistenceMapper;
    private final EntityManager entityManager;

    RequestPersistenceAdapter(RequestJpaRepository requestJpaRepository,
                              RequestPersistenceMapper requestPersistenceMapper,
                              EntityManager entityManager) {
        this.requestJpaRepository = Objects.requireNonNull(requestJpaRepository);
        this.requestPersistenceMapper = Objects.requireNonNull(requestPersistenceMapper);
        this.entityManager = Objects.requireNonNull(entityManager);
    }

    @Override
    public void save(AcademicRequest request) {
        Objects.requireNonNull(request, "La solicitud no puede ser null");

        var applicantReference = entityManager.getReference(UserJpaEntity.class, request.getApplicantId().value());
        var responsibleReference = request.getResponsibleId() == null
                ? null
                : entityManager.getReference(UserJpaEntity.class, request.getResponsibleId().value());
        var originChannelReference = entityManager.getReference(OriginChannelJpaEntity.class, request.getOriginChannelId().value());
        var requestTypeReference = entityManager.getReference(RequestTypeJpaEntity.class, request.getRequestTypeId().value());

        var entity = requestPersistenceMapper.toEntity(
                request,
                applicantReference,
                responsibleReference,
                originChannelReference,
                requestTypeReference,
                businessRuleId -> entityManager.getReference(BusinessRuleJpaEntity.class, businessRuleId),
                userId -> entityManager.getReference(UserJpaEntity.class, userId)
        );

        if (requestJpaRepository.existsById(request.getId().value())) {
            requestJpaRepository.save(entity);
            return;
        }

        var reservedRequestId = request.getId().value();
        entity.setId(null);
        entity.getHistory().forEach(historyEntry -> historyEntry.setId(null));
        entityManager.persist(entity);
        entityManager.flush();

        if (!Objects.equals(entity.getId(), reservedRequestId)) {
            throw new IllegalStateException("El id reservado para la solicitud no coincidió con el generado por la base de datos");
        }
    }

    @Override
    public Optional<AcademicRequest> loadById(RequestId requestId) {
        return requestJpaRepository.findDetailedById(requestId.value()).map(requestPersistenceMapper::toDomain);
    }

    @Override
    public Optional<RequestDetail> loadDetailById(RequestId requestId) {
        return requestJpaRepository.findDetailedById(requestId.value()).map(requestPersistenceMapper::toDetail);
    }

    @Override
    public Page<RequestSummary> search(RequestSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "Los criterios de búsqueda no pueden ser null");

        var page = requestJpaRepository.findAll(
                RequestSpecifications.withCriteria(criteria),
                PageRequest.of(criteria.page(), criteria.size(), toSort(criteria.sort()))
        );

        var content = page.getContent().stream()
                .map(requestPersistenceMapper::toSummary)
                .toList();

        return new Page<>(
                content,
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    private Sort toSort(String sortExpression) {
        var segments = sortExpression.split(",", 2);
        if (segments.length != 2) {
            throw new IllegalArgumentException("El sort debe incluir campo y dirección separados por coma");
        }

        var requestedField = segments[0].trim();
        var directionToken = segments[1].trim();
        var property = SORT_FIELDS.get(requestedField);
        if (property == null) {
            throw new IllegalArgumentException("Campo de ordenamiento no soportado: " + requestedField);
        }

        var direction = Sort.Direction.fromOptionalString(directionToken)
                .orElseThrow(() -> new IllegalArgumentException("Dirección de sort inválida: " + directionToken));
        if ("id".equals(property)) {
            return Sort.by(direction, property);
        }

        return Sort.by(
                new Sort.Order(direction, property),
                new Sort.Order(direction, "id")
        );
    }
}
