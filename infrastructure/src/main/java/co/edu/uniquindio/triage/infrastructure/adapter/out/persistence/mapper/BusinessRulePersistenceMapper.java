package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper;

import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.BusinessRuleJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface BusinessRulePersistenceMapper {

    @Mapping(target = "id", source = "id", qualifiedByName = "toBusinessRuleId")
    @Mapping(target = "requestTypeId", source = "requestType", qualifiedByName = "toRequestTypeId")
    BusinessRule toDomain(BusinessRuleJpaEntity entity);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "requestType", source = "requestTypeId", qualifiedByName = "toRequestTypeEntity")
    BusinessRuleJpaEntity toEntity(BusinessRule businessRule);

    @Named("toBusinessRuleId")
    default BusinessRuleId toBusinessRuleId(Long id) {
        return id != null ? new BusinessRuleId(id) : null;
    }

    @Named("toRequestTypeId")
    default RequestTypeId toRequestTypeId(RequestTypeJpaEntity entity) {
        return entity != null ? new RequestTypeId(entity.getId()) : null;
    }

    @Named("toRequestTypeEntity")
    default RequestTypeJpaEntity toRequestTypeEntity(RequestTypeId id) {
        if (id == null) return null;
        RequestTypeJpaEntity entity = new RequestTypeJpaEntity();
        entity.setId(id.value());
        return entity;
    }
}
