package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.businessrule.UpdateBusinessRuleUseCase;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.UpdateBusinessRuleCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveBusinessRulePort;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.BusinessRule;

import java.util.Objects;

public class UpdateBusinessRuleService implements UpdateBusinessRuleUseCase {

    private final SaveBusinessRulePort saveBusinessRulePort;
    private final LoadBusinessRulePort loadBusinessRulePort;
    private final LoadRequestTypePort loadRequestTypePort;

    public UpdateBusinessRuleService(SaveBusinessRulePort saveBusinessRulePort,
                                     LoadBusinessRulePort loadBusinessRulePort,
                                     LoadRequestTypePort loadRequestTypePort) {
        this.saveBusinessRulePort = saveBusinessRulePort;
        this.loadBusinessRulePort = loadBusinessRulePort;
        this.loadRequestTypePort = loadRequestTypePort;
    }

    @Override
    public BusinessRule update(UpdateBusinessRuleCommand command) {
        Objects.requireNonNull(command, "El command no puede ser null");

        BusinessRule businessRule = loadBusinessRulePort.findById(command.id())
                .orElseThrow(() -> new EntityNotFoundException("Regla de negocio", "id", command.id().value()));

        if (!businessRule.getName().equalsIgnoreCase(command.name()) && loadBusinessRulePort.existsByName(command.name())) {
            throw new DuplicateCatalogEntryException("regla de negocio", command.name());
        }

        if (command.requestTypeId() != null) {
            loadRequestTypePort.loadById(command.requestTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("Tipo de solicitud", "id", command.requestTypeId().value()));
        }

        businessRule.update(
                command.name(),
                command.description(),
                command.conditionType(),
                command.conditionValue(),
                command.resultingPriority(),
                command.requestTypeId(),
                command.active()
        );

        return saveBusinessRulePort.save(businessRule);
    }
}
