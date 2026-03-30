package co.edu.uniquindio.triage.application.port.in.businessrule;

import co.edu.uniquindio.triage.application.port.in.command.businessrule.ListBusinessRulesQuery;
import co.edu.uniquindio.triage.domain.model.BusinessRule;

import java.util.List;

public interface ListBusinessRulesQueryUseCase {
    List<BusinessRule> list(ListBusinessRulesQuery query);
}
