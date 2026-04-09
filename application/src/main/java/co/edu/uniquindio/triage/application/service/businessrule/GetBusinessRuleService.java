package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.businessrule.BusinessRuleView;
import co.edu.uniquindio.triage.application.port.in.businessrule.GetBusinessRuleQueryUseCase;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;

import java.util.Objects;
import java.util.Optional;

public class GetBusinessRuleService implements GetBusinessRuleQueryUseCase {

    private final LoadBusinessRulePort loadBusinessRulePort;
    private final BusinessRuleViewSupport businessRuleViewSupport;

    public GetBusinessRuleService(LoadBusinessRulePort loadBusinessRulePort,
                                  BusinessRuleViewSupport businessRuleViewSupport) {
        this.loadBusinessRulePort = Objects.requireNonNull(loadBusinessRulePort, "loadBusinessRulePort no puede ser null");
        this.businessRuleViewSupport = Objects.requireNonNull(businessRuleViewSupport, "businessRuleViewSupport no puede ser null");
    }

    @Override
    public Optional<BusinessRuleView> getById(BusinessRuleId id) {
        return loadBusinessRulePort.findById(id).map(businessRuleViewSupport::hydrate);
    }
}
