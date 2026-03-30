package co.edu.uniquindio.triage.application.port.in.businessrule;

import co.edu.uniquindio.triage.application.port.in.command.businessrule.DeactivateBusinessRuleCommand;

public interface DeactivateBusinessRuleUseCase {
    void deactivate(DeactivateBusinessRuleCommand command);
}
