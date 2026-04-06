package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.businessrule.ListBusinessRulesQueryUseCase;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.ListBusinessRulesQuery;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.domain.model.BusinessRule;

import java.util.List;
import java.util.Objects;

public class ListBusinessRulesService implements ListBusinessRulesQueryUseCase {

    private final LoadBusinessRulePort loadBusinessRulePort;

    public ListBusinessRulesService(LoadBusinessRulePort loadBusinessRulePort) {
        this.loadBusinessRulePort = loadBusinessRulePort;
    }

    @Override
    public List<BusinessRule> list(ListBusinessRulesQuery query) {
        Objects.requireNonNull(query, "El query no puede ser null");
        return loadBusinessRulePort.findAll(query.active(), query.conditionType());
    }
}
