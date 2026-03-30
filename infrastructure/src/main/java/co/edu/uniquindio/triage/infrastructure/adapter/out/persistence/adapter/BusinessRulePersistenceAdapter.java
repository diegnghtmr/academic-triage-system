package co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.adapter;

import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveBusinessRulePort;
import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.mapper.BusinessRulePersistenceMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.out.persistence.repository.BusinessRuleJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
class BusinessRulePersistenceAdapter implements LoadBusinessRulePort, SaveBusinessRulePort {

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
        var entity = businessRulePersistenceMapper.toEntity(businessRule);
        var saved = businessRuleJpaRepository.save(entity);
        return businessRulePersistenceMapper.toDomain(saved);
    }
}
