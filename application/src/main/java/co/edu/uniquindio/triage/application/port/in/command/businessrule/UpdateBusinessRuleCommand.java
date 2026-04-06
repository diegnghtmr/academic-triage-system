package co.edu.uniquindio.triage.application.port.in.command.businessrule;

import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.util.Objects;

public record UpdateBusinessRuleCommand(
        BusinessRuleId id,
        String name,
        String description,
        ConditionType conditionType,
        String conditionValue,
        Priority resultingPriority,
        RequestTypeId requestTypeId,
        boolean active
) {
    public UpdateBusinessRuleCommand {
        Objects.requireNonNull(id, "El id no puede ser null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser null o vacío");
        }
        if (name.trim().length() > 150) {
            throw new IllegalArgumentException("El nombre no puede tener más de 150 caracteres");
        }
        Objects.requireNonNull(conditionType, "El conditionType no puede ser null");
        if (conditionValue == null || conditionValue.isBlank()) {
            throw new IllegalArgumentException("El conditionValue no puede ser null o vacío");
        }
        Objects.requireNonNull(resultingPriority, "El resultingPriority no puede ser null");
    }
}
