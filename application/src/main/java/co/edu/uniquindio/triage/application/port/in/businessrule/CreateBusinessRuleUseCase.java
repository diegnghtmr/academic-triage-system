package co.edu.uniquindio.triage.application.port.in.businessrule;

import co.edu.uniquindio.triage.application.port.in.command.businessrule.CreateBusinessRuleCommand;
import co.edu.uniquindio.triage.domain.model.BusinessRule;

public interface CreateBusinessRuleUseCase {
    BusinessRule create(CreateBusinessRuleCommand command);
}
