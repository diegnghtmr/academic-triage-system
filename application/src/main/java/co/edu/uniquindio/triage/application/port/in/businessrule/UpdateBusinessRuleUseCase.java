package co.edu.uniquindio.triage.application.port.in.businessrule;

import co.edu.uniquindio.triage.application.port.in.command.businessrule.UpdateBusinessRuleCommand;

public interface UpdateBusinessRuleUseCase {
    BusinessRuleView update(UpdateBusinessRuleCommand command);
}
