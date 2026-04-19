package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRuleVersionPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveBusinessRulePort;
import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.BusinessRuleJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.entity.RequestTypeJpaEntity;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.BusinessRulePersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.BusinessRuleJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
class BusinessRulePersistenceAdapter implements LoadBusinessRulePort, SaveBusinessRulePort, LoadBusinessRuleVersionPort {

    private final BusinessRuleJpaRepository businessRuleJpaRepository;
    private final BusinessRulePersistenceMapper businessRulePersistenceMapper;

    BusinessRulePersistenceAdapter(BusinessRuleJpaRepository businessRuleJpaRepository,
                                  BusinessRulePersistenceMapper businessRulePersistenceMapper) {
        this.businessRuleJpaRepository = Objects.requireNonNull(businessRuleJpaRepository);
        this.businessRulePersistenceMapper = Objects.requireNonNull(businessRulePersistenceMapper);
    }

    @Override
    public Optional<BusinessRule> findById(BusinessRuleId id) {
        return businessRuleJpaRepository.findById(id.value())
                .map(businessRulePersistenceMapper::toDomain);
    }

    @Override
    public List<BusinessRule> findAll(Boolean active, ConditionType conditionType) {
        String typeStr = conditionType != null ? conditionType.name() : null;
        
        if (active != null && conditionType != null) {
            return businessRuleJpaRepository.findAllByActiveAndConditionType(active, typeStr)
                    .stream().map(businessRulePersistenceMapper::toDomain).toList();
        } else if (active != null) {
            return businessRuleJpaRepository.findAllByActive(active)
                    .stream().map(businessRulePersistenceMapper::toDomain).toList();
        } else if (conditionType != null) {
            return businessRuleJpaRepository.findAllByConditionType(typeStr)
                    .stream().map(businessRulePersistenceMapper::toDomain).toList();
        } else {
            return businessRuleJpaRepository.findAll()
                    .stream().map(businessRulePersistenceMapper::toDomain).toList();
        }
    }

    @Override
    public boolean existsByName(String name) {
        return businessRuleJpaRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, BusinessRuleId id) {
        return businessRuleJpaRepository.existsByNameIgnoreCaseAndIdNot(name, id.value());
    }

    @Override
    public BusinessRule save(BusinessRule businessRule) {
        BusinessRuleJpaEntity entity;
        if (businessRule.getId() != null) {
            // Update-in-place: preserves @Version so optimistic locking is enforced correctly
            entity = businessRuleJpaRepository.findById(businessRule.getId().value())
                    .orElseThrow(() -> new EntityNotFoundException("Regla de negocio", "id", businessRule.getId().value()));
            applyDomainToEntity(businessRule, entity);
        } else {
            entity = businessRulePersistenceMapper.toEntity(businessRule);
        }
        var saved = businessRuleJpaRepository.save(entity);
        return businessRulePersistenceMapper.toDomain(saved);
    }

    private void applyDomainToEntity(BusinessRule rule, BusinessRuleJpaEntity entity) {
        entity.setName(rule.getName());
        entity.setDescription(rule.getDescription());
        if (rule.getConditionType() != null) {
            entity.setConditionType(rule.getConditionType().name());
        }
        entity.setConditionValue(rule.getConditionValue());
        if (rule.getResultingPriority() != null) {
            entity.setResultingPriority(rule.getResultingPriority().name());
        }
        entity.setActive(rule.isActive());
        if (rule.getRequestTypeId() != null) {
            var rt = new RequestTypeJpaEntity();
            rt.setId(rule.getRequestTypeId().value());
            entity.setRequestType(rt);
        } else {
            entity.setRequestType(null);
        }
    }

    @Override
    public Optional<Long> findVersionById(BusinessRuleId id) {
        return businessRuleJpaRepository.findById(id.value())
                .map(BusinessRuleJpaEntity::getVersion);
    }
}
