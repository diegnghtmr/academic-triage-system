package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.businessrule.DeactivateBusinessRuleUseCase;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.DeactivateBusinessRuleCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveBusinessRulePort;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.BusinessRule;

import java.util.Objects;

public class DeactivateBusinessRuleService implements DeactivateBusinessRuleUseCase {

    private final LoadBusinessRulePort loadBusinessRulePort;
    private final SaveBusinessRulePort saveBusinessRulePort;

    public DeactivateBusinessRuleService(LoadBusinessRulePort loadBusinessRulePort,
                                         SaveBusinessRulePort saveBusinessRulePort) {
        this.loadBusinessRulePort = loadBusinessRulePort;
        this.saveBusinessRulePort = saveBusinessRulePort;
    }

    @Override
    public void deactivate(DeactivateBusinessRuleCommand command) {
        Objects.requireNonNull(command, "El command no puede ser null");

        BusinessRule businessRule = loadBusinessRulePort.findById(command.id())
                .orElseThrow(() -> new EntityNotFoundException("Regla de negocio", "id", command.id().value()));

        businessRule.deactivate();
        saveBusinessRulePort.save(businessRule);
    }
}
