package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;

import java.util.List;
import java.util.Optional;

public interface LoadBusinessRulePort {
    Optional<BusinessRule> findById(BusinessRuleId id);
    List<BusinessRule> findAll(Boolean active, ConditionType conditionType);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, BusinessRuleId id);
}
