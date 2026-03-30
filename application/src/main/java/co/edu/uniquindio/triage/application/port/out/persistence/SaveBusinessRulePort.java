package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.BusinessRule;

public interface SaveBusinessRulePort {
    BusinessRule save(BusinessRule businessRule);
}
