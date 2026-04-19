package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;

import java.util.Optional;

public interface LoadBusinessRuleVersionPort {
    Optional<Long> findVersionById(BusinessRuleId id);
}
