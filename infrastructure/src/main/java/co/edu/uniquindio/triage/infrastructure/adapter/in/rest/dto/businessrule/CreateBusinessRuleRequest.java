package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule;

import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBusinessRuleRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder los 150 caracteres")
        String name,

        @Size(max = 500, message = "La descripción no puede exceder los 500 caracteres")
        String description,

        @NotNull(message = "El tipo de condición es obligatorio")
        ConditionType conditionType,

        @NotBlank(message = "El valor de la condición es obligatorio")
        String conditionValue,

        @NotNull(message = "La prioridad resultante es obligatoria")
        Priority resultingPriority,

        Long requestTypeId,

        Boolean active
) {
}
