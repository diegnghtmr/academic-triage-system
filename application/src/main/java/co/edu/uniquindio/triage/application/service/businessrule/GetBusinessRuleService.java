package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.businessrule.GetBusinessRuleQueryUseCase;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;

import java.util.Optional;

public class GetBusinessRuleService implements GetBusinessRuleQueryUseCase {

    private final LoadBusinessRulePort loadBusinessRulePort;

    public GetBusinessRuleService(LoadBusinessRulePort loadBusinessRulePort) {
        this.loadBusinessRulePort = loadBusinessRulePort;
    }

    @Override
    public Optional<BusinessRule> getById(BusinessRuleId id) {
        return loadBusinessRulePort.findById(id);
    }
}
