package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.domain.enums.ConditionType;

/**
 * Parses business-rule condition types from HTTP (JSON or query), rejecting the retired IMPACT_LEVEL
 * contract with an explicit, stable message for legacy clients.
 */
final class BusinessRuleConditionTypeParser {

    static final String LEGACY_IMPACT_LEVEL_REJECTED =
            "El tipo de condición IMPACT_LEVEL ya no está soportado. Use REQUEST_TYPE, DEADLINE o "
                    + "REQUEST_TYPE_AND_DEADLINE.";

    private BusinessRuleConditionTypeParser() {
    }

    static ConditionType parseRequired(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("El tipo de condición es obligatorio");
        }
        var normalized = raw.trim();
        if (normalized.equalsIgnoreCase("IMPACT_LEVEL")) {
            throw new IllegalArgumentException(LEGACY_IMPACT_LEVEL_REJECTED);
        }
        try {
            return ConditionType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Tipo de condición inválido: '%s'. Valores permitidos: REQUEST_TYPE, DEADLINE, REQUEST_TYPE_AND_DEADLINE."
                            .formatted(normalized));
        }
    }
}
