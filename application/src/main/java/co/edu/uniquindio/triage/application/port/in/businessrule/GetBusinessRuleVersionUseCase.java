package co.edu.uniquindio.triage.application.port.in.businessrule;

import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;

import java.util.Optional;

public interface GetBusinessRuleVersionUseCase {
    Optional<Long> getVersionById(BusinessRuleId id);
}
