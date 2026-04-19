package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.in.common.Page;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;
import co.edu.uniquindio.triage.application.port.out.persistence.CreateRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestForMutationPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestVersionPort;
import co.edu.uniquindio.triage.application.port.out.persistence.NewAcademicRequest;
import co.edu.uniquindio.triage.application.port.out.persistence.RequestSearchCriteria;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SearchRequestPort;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.AcademicRequestJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.BusinessRuleJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.OriginChannelJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.RequestJpaRepository;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.specification.RequestSpecifications;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ArrayList;

@Component
class RequestPersistenceAdapter implements CreateRequestPort, SaveRequestPort, LoadRequestPort, LoadRequestForMutationPort, SearchRequestPort, LoadRequestVersionPort {

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
    @Transactional
    public AcademicRequest create(NewAcademicRequest request) {
        Objects.requireNonNull(request, "La solicitud no puede ser null");

        var applicantReference = entityManager.getReference(UserJpaEntity.class, request.applicantId().value());
        var originChannelReference = entityManager.getReference(OriginChannelJpaEntity.class, request.originChannelId().value());
        var requestTypeReference = entityManager.getReference(RequestTypeJpaEntity.class, request.requestTypeId().value());
        var performedByReference = entityManager.getReference(UserJpaEntity.class, request.applicantId().value());

        var entity = new AcademicRequestJpaEntity();
        entity.setDescription(request.description());
        entity.setStatus(RequestStatus.REGISTERED.name());
        entity.setDeadline(request.deadline());
        entity.setRegistrationDateTime(request.registrationDateTime());
        entity.setAiSuggested(request.aiSuggested());
        entity.setApplicant(applicantReference);
        entity.setOriginChannel(originChannelReference);
        entity.setRequestType(requestTypeReference);
        entityManager.persist(entity);
        entityManager.flush();

        var aggregate = new AcademicRequest(
                new RequestId(entity.getId()),
                request.description(),
                request.applicantId(),
                request.originChannelId(),
                request.requestTypeId(),
                request.deadline(),
                request.aiSuggested(),
                request.registrationDateTime()
        );

        var persistedState = requestPersistenceMapper.toEntity(
                aggregate,
                applicantReference,
                null,
                originChannelReference,
                requestTypeReference,
                businessRuleId -> entityManager.getReference(BusinessRuleJpaEntity.class, businessRuleId),
                userId -> userId.equals(request.applicantId().value())
                        ? performedByReference
                        : entityManager.getReference(UserJpaEntity.class, userId)
        );

        var history = new ArrayList<>(persistedState.getHistory());
        history.forEach(entry -> entry.setRequest(entity));
        entity.getHistory().clear();
        entity.getHistory().addAll(history);
        entityManager.flush();

        return aggregate;
    }

    @Override
    @Transactional
    public void save(AcademicRequest request) {
        Objects.requireNonNull(request, "La solicitud no puede ser null");

        Long currentVersion = requestJpaRepository.findById(request.getId().value())
                .map(AcademicRequestJpaEntity::getVersion)
                .orElseThrow(() -> new IllegalStateException("No se puede actualizar una solicitud inexistente con save(); use create() para nuevas solicitudes"));

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
        entity.setVersion(currentVersion);

        entityManager.merge(entity);
        entityManager.flush();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AcademicRequest> loadById(RequestId requestId) {
        return requestJpaRepository.findDetailedById(requestId.value()).map(requestPersistenceMapper::toDomain);
    }

    /**
     * Loads the aggregate under a PESSIMISTIC_WRITE lock (SELECT … FOR UPDATE).
     * Must be called from within an active @Transactional boundary so that the lock
     * is held until the enclosing transaction commits — preventing lost-update races.
     */
    @Override
    @Transactional
    public Optional<AcademicRequest> loadByIdForMutation(RequestId requestId) {
        // Acquire X lock on the main row — blocks concurrent mutations on the same request
        var locked = requestJpaRepository.findByIdForUpdate(requestId.value());
        if (locked.isEmpty()) {
            return Optional.empty();
        }
        // Load with full associations (history, etc.) within the same transaction
        return requestJpaRepository.findDetailedById(requestId.value())
                .map(requestPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RequestDetail> loadDetailById(RequestId requestId) {
        return requestJpaRepository.findDetailedById(requestId.value()).map(requestPersistenceMapper::toDetail);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findVersionById(RequestId requestId) {
        return requestJpaRepository.findById(requestId.value())
                .map(AcademicRequestJpaEntity::getVersion);
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
