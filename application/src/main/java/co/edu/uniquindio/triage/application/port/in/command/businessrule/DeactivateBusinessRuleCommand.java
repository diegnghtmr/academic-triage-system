package co.edu.uniquindio.triage.application.port.in.command.businessrule;

import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;

import java.util.Objects;

public record DeactivateBusinessRuleCommand(BusinessRuleId id) {
    public DeactivateBusinessRuleCommand {
        Objects.requireNonNull(id, "El id no puede ser null");
    }
}
