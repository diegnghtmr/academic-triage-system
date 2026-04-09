package co.edu.uniquindio.triage.application.port.in.businessrule;

import co.edu.uniquindio.triage.application.port.in.command.businessrule.CreateBusinessRuleCommand;

public interface CreateBusinessRuleUseCase {
    BusinessRuleView create(CreateBusinessRuleCommand command);
}
