package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.businessrule.BusinessRuleView;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.domain.model.BusinessRule;

import java.util.Objects;

public class BusinessRuleViewSupport {

    private final LoadRequestTypePort loadRequestTypePort;

    public BusinessRuleViewSupport(LoadRequestTypePort loadRequestTypePort) {
        this.loadRequestTypePort = Objects.requireNonNull(loadRequestTypePort, "loadRequestTypePort no puede ser null");
    }

    public BusinessRuleView hydrate(BusinessRule rule) {
        Objects.requireNonNull(rule, "rule no puede ser null");
        var requestType = rule.getRequestTypeId() != null
                ? loadRequestTypePort.loadById(rule.getRequestTypeId()).orElse(null)
                : null;
        return new BusinessRuleView(rule, requestType);
    }
}
