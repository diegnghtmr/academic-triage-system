package co.edu.uniquindio.triage.application.port.in.businessrule;

import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;

import java.util.Optional;

public interface GetBusinessRuleQueryUseCase {
    Optional<BusinessRule> getById(BusinessRuleId id);
}
