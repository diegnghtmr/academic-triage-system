package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper;

import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.in.request.RequestHistoryDetail;
import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;
import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.RequestStatus;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.RequestHistory;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestHistoryId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.AcademicRequestJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.BusinessRuleJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.OriginChannelJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestHistoryJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestRuleJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Component
public class RequestPersistenceMapper {

    // This mapper stays manual for now because it reconstructs the aggregate root,
    // normalizes ordered history, resolves JPA references through functions, and
    // projects the same entity into multiple application-layer views.

    private final UserPersistenceMapper userPersistenceMapper;
    private final CatalogPersistenceMapper catalogPersistenceMapper;

    public RequestPersistenceMapper(UserPersistenceMapper userPersistenceMapper,
                                    CatalogPersistenceMapper catalogPersistenceMapper) {
        this.userPersistenceMapper = userPersistenceMapper;
        this.catalogPersistenceMapper = catalogPersistenceMapper;
    }

    public AcademicRequest toDomain(AcademicRequestJpaEntity entity) {
        var appliedRuleIds = List.<BusinessRuleId>of();
        var history = orderedHistory(entity.getHistory()).stream()
                .map(this::toHistoryDomain)
                .toList();

        return AcademicRequest.reconstitute(
                new RequestId(entity.getId()),
                entity.getDescription(),
                RequestStatus.valueOf(entity.getStatus()),
                entity.getPriority() == null ? null : Priority.valueOf(entity.getPriority()),
                entity.getPriorityJustification(),
                entity.getDeadline(),
                entity.getRegistrationDateTime(),
                entity.isAiSuggested(),
                entity.getRejectionReason(),
                entity.getClosingObservation(),
                entity.getCancellationReason(),
                entity.getAttendanceObservation(),
                new UserId(entity.getApplicant().getId()),
                entity.getResponsible() == null ? null : new UserId(entity.getResponsible().getId()),
                new OriginChannelId(entity.getOriginChannel().getId()),
                new RequestTypeId(entity.getRequestType().getId()),
                appliedRuleIds,
                history
        );
    }

    public AcademicRequestJpaEntity toEntity(AcademicRequest request,
                                             UserJpaEntity applicant,
                                             UserJpaEntity responsible,
                                             OriginChannelJpaEntity originChannel,
                                             RequestTypeJpaEntity requestType,
                                             Function<Long, BusinessRuleJpaEntity> businessRuleReferenceProvider,
                                             Function<Long, UserJpaEntity> userReferenceProvider) {
        var entity = new AcademicRequestJpaEntity();
        entity.setId(request.getId().value());
        entity.setDescription(request.getDescription());
        entity.setPriority(request.getPriority() == null ? null : request.getPriority().name());
        entity.setStatus(request.getStatus().name());
        entity.setDeadline(request.getDeadline());
        entity.setRegistrationDateTime(request.getRegistrationDateTime());
        entity.setPriorityJustification(request.getPriorityJustification());
        entity.setRejectionReason(request.getRejectionReason());
        entity.setAiSuggested(request.isAiSuggested());
        entity.setClosingObservation(request.getClosingObservation());
        entity.setCancellationReason(request.getCancellationReason());
        entity.setAttendanceObservation(request.getAttendanceObservation());
        entity.setCreatedAt(request.getRegistrationDateTime());
        entity.setUpdatedAt(request.getRegistrationDateTime());
        entity.setApplicant(applicant);
        entity.setResponsible(responsible);
        entity.setOriginChannel(originChannel);
        entity.setRequestType(requestType);

        var history = request.getHistory().stream()
                .map(historyEntry -> toHistoryEntity(historyEntry, entity, userReferenceProvider.apply(historyEntry.getPerformedById().value())))
                .toList();
        entity.setHistory(history);

        var appliedRules = request.getAppliedRuleIds().stream()
                .map(ruleId -> {
                    var requestRule = new RequestRuleJpaEntity();
                    requestRule.setRequest(entity);
                    requestRule.setRule(businessRuleReferenceProvider.apply(ruleId.value()));
                    return requestRule;
                })
                .toList();
        entity.setAppliedRules(appliedRules);

        return entity;
    }

    public RequestSummary toSummary(AcademicRequestJpaEntity entity) {
        return new RequestSummary(
                AcademicRequest.reconstitute(
                        new RequestId(entity.getId()),
                        entity.getDescription(),
                        RequestStatus.valueOf(entity.getStatus()),
                        entity.getPriority() == null ? null : Priority.valueOf(entity.getPriority()),
                        entity.getPriorityJustification(),
                        entity.getDeadline(),
                        entity.getRegistrationDateTime(),
                        entity.isAiSuggested(),
                        entity.getRejectionReason(),
                        entity.getClosingObservation(),
                        entity.getCancellationReason(),
                        entity.getAttendanceObservation(),
                        new UserId(entity.getApplicant().getId()),
                        entity.getResponsible() == null ? null : new UserId(entity.getResponsible().getId()),
                        new OriginChannelId(entity.getOriginChannel().getId()),
                        new RequestTypeId(entity.getRequestType().getId()),
                        List.of(),
                        List.of()
                ),
                catalogPersistenceMapper.toDomain(entity.getRequestType()),
                catalogPersistenceMapper.toDomain(entity.getOriginChannel()),
                userPersistenceMapper.toDomain(entity.getApplicant()),
                Optional.ofNullable(entity.getResponsible()).map(userPersistenceMapper::toDomain)
        );
    }

    public RequestDetail toDetail(AcademicRequestJpaEntity entity) {
        var history = orderedHistory(entity.getHistory()).stream()
                .map(historyEntry -> new RequestHistoryDetail(
                        toHistoryDomain(historyEntry),
                        userPersistenceMapper.toDomain(historyEntry.getPerformedBy())
                ))
                .toList();

        return new RequestDetail(
                toDomain(entity),
                catalogPersistenceMapper.toDomain(entity.getRequestType()),
                catalogPersistenceMapper.toDomain(entity.getOriginChannel()),
                userPersistenceMapper.toDomain(entity.getApplicant()),
                Optional.ofNullable(entity.getResponsible()).map(userPersistenceMapper::toDomain),
                history
        );
    }

    private RequestHistory toHistoryDomain(RequestHistoryJpaEntity entity) {
        return new RequestHistory(
                entity.getId() == null ? null : new RequestHistoryId(entity.getId()),
                HistoryAction.valueOf(entity.getAction()),
                entity.getObservations(),
                entity.getTimestamp(),
                new RequestId(entity.getRequest().getId()),
                new UserId(entity.getPerformedBy().getId())
        );
    }

    private RequestHistoryJpaEntity toHistoryEntity(RequestHistory history,
                                                    AcademicRequestJpaEntity request,
                                                    UserJpaEntity performedBy) {
        var entity = new RequestHistoryJpaEntity();
        entity.setId(history.getId() == null ? null : history.getId().value());
        entity.setAction(history.getAction().name());
        entity.setObservations(history.getObservations());
        entity.setTimestamp(history.getTimestamp());
        entity.setRequest(request);
        entity.setPerformedBy(performedBy);
        return entity;
    }

    private List<RequestHistoryJpaEntity> orderedHistory(List<RequestHistoryJpaEntity> history) {
        return history.stream()
                .sorted(Comparator.comparing(RequestHistoryJpaEntity::getTimestamp)
                        .thenComparing(entry -> entry.getId() == null ? Long.MAX_VALUE : entry.getId()))
                .toList();
    }
}
