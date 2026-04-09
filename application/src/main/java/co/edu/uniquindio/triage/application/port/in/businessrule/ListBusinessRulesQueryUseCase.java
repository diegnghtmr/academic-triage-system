package co.edu.uniquindio.triage.application.port.in.businessrule;

import co.edu.uniquindio.triage.application.port.in.command.businessrule.ListBusinessRulesQuery;

import java.util.List;

public interface ListBusinessRulesQueryUseCase {
    List<BusinessRuleView> list(ListBusinessRulesQuery query);
}
