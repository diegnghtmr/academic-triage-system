package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.businessrule.BusinessRuleView;
import co.edu.uniquindio.triage.application.port.in.businessrule.ListBusinessRulesQueryUseCase;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.ListBusinessRulesQuery;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;

import java.util.List;
import java.util.Objects;

public class ListBusinessRulesService implements ListBusinessRulesQueryUseCase {

    private final LoadBusinessRulePort loadBusinessRulePort;
    private final BusinessRuleViewSupport businessRuleViewSupport;

    public ListBusinessRulesService(LoadBusinessRulePort loadBusinessRulePort,
                                    BusinessRuleViewSupport businessRuleViewSupport) {
        this.loadBusinessRulePort = Objects.requireNonNull(loadBusinessRulePort, "loadBusinessRulePort no puede ser null");
        this.businessRuleViewSupport = Objects.requireNonNull(businessRuleViewSupport, "businessRuleViewSupport no puede ser null");
    }

    @Override
    public List<BusinessRuleView> list(ListBusinessRulesQuery query) {
        Objects.requireNonNull(query, "El query no puede ser null");
        return loadBusinessRulePort.findAll(query.active(), query.conditionType()).stream()
                .map(businessRuleViewSupport::hydrate)
                .toList();
    }
}
