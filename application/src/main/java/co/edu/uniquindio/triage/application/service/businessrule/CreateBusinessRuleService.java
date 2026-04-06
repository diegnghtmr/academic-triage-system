package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.businessrule.CreateBusinessRuleUseCase;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.CreateBusinessRuleCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveBusinessRulePort;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.BusinessRule;

import java.util.Objects;

public class CreateBusinessRuleService implements CreateBusinessRuleUseCase {

    private final SaveBusinessRulePort saveBusinessRulePort;
    private final LoadBusinessRulePort loadBusinessRulePort;
    private final LoadRequestTypePort loadRequestTypePort;

    public CreateBusinessRuleService(SaveBusinessRulePort saveBusinessRulePort,
                                     LoadBusinessRulePort loadBusinessRulePort,
                                     LoadRequestTypePort loadRequestTypePort) {
        this.saveBusinessRulePort = saveBusinessRulePort;
        this.loadBusinessRulePort = loadBusinessRulePort;
        this.loadRequestTypePort = loadRequestTypePort;
    }

    @Override
    public BusinessRule create(CreateBusinessRuleCommand command) {
        Objects.requireNonNull(command, "El command no puede ser null");

        if (loadBusinessRulePort.existsByName(command.name())) {
            throw new DuplicateCatalogEntryException("regla de negocio", command.name());
        }

        if (command.requestTypeId() != null) {
            loadRequestTypePort.loadById(command.requestTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("Tipo de solicitud", "id", command.requestTypeId().value()));
        }

        BusinessRule businessRule = BusinessRule.createNew(
                command.name(),
                command.description(),
                command.conditionType(),
                command.conditionValue(),
                command.resultingPriority(),
                command.requestTypeId()
        );

        return saveBusinessRulePort.save(businessRule);
    }
}
