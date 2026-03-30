package co.edu.uniquindio.triage.application.port.in.businessrule;

import co.edu.uniquindio.triage.application.port.in.command.businessrule.UpdateBusinessRuleCommand;
import co.edu.uniquindio.triage.domain.model.BusinessRule;

public interface UpdateBusinessRuleUseCase {
    BusinessRule update(UpdateBusinessRuleCommand command);
}
