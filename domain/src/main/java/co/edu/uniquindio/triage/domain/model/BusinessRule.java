package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class BusinessRule {
    private final BusinessRuleId id;
    private String name;
    private String description;
    private ConditionType conditionType;
    private String conditionValue;
    private Priority resultingPriority;
    private boolean active;
    private RequestTypeId requestTypeId;

    public BusinessRule(BusinessRuleId id, String name, String description,
                        ConditionType conditionType, String conditionValue,
                        Priority resultingPriority, boolean active, RequestTypeId requestTypeId) {
        this.id = id; // Can be null for new rules
        this.name = validateName(name);
        this.description = validateDescription(description);
        this.conditionType = Objects.requireNonNull(conditionType, "El conditionType no puede ser null");
        this.conditionValue = validateConditionValue(conditionValue);
        this.resultingPriority = Objects.requireNonNull(resultingPriority, "El resultingPriority no puede ser null");
        this.active = active;
        this.requestTypeId = requestTypeId;
    }

    /**
     * Creates a new BusinessRule with the given details and default active=true.
     */
    public static BusinessRule createNew(String name, String description,
                                         ConditionType conditionType, String conditionValue,
                                         Priority resultingPriority, RequestTypeId requestTypeId) {
        return new BusinessRule(null, name, description, conditionType, conditionValue,
                resultingPriority, true, requestTypeId);
    }

    public static BusinessRule reconstitute(BusinessRuleId id, String name, String description,
                                            ConditionType conditionType, String conditionValue,
                                            Priority resultingPriority, RequestTypeId requestTypeId,
                                            boolean active) {
        return new BusinessRule(Objects.requireNonNull(id, "El id no puede ser null"),
                name, description, conditionType, conditionValue, resultingPriority, active, requestTypeId);
    }

    public void update(String name, String description, ConditionType conditionType,
                       String conditionValue, Priority resultingPriority, RequestTypeId requestTypeId,
                       boolean active) {
        this.name = validateName(name);
        this.description = validateDescription(description);
        this.conditionType = Objects.requireNonNull(conditionType, "El conditionType no puede ser null");
        this.conditionValue = validateConditionValue(conditionValue);
        this.resultingPriority = Objects.requireNonNull(resultingPriority, "El resultingPriority no puede ser null");
        this.requestTypeId = requestTypeId;
        this.active = active;
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser null o vacío");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 150) {
            throw new IllegalArgumentException("El nombre no puede tener más de 150 caracteres");
        }
        return trimmed;
    }

    private String validateDescription(String description) {
        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException("La descripción no puede tener más de 500 caracteres");
        }
        return description != null ? description.trim() : null;
    }

    private String validateConditionValue(String conditionValue) {
        if (conditionValue == null || conditionValue.isBlank()) {
            throw new IllegalArgumentException("El conditionValue no puede ser null o vacío");
        }
        // Relaxation of length for opaque JSON/text
        return conditionValue.trim();
    }

    public BusinessRuleId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void updateName(String name) {
        this.name = validateName(name);
    }

    public String getDescription() {
        return description;
    }

    public void updateDescription(String description) {
        this.description = validateDescription(description);
    }

    public ConditionType getConditionType() {
        return conditionType;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public Priority getResultingPriority() {
        return resultingPriority;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    public RequestTypeId getRequestTypeId() {
        return requestTypeId;
    }

    /**
     * Evaluates whether this rule's condition matches the given request.
     */
    public boolean matches(AcademicRequest request) {
        Objects.requireNonNull(request, "La solicitud no puede ser null");
        if (!this.active) {
            return false;
        }

        return switch (this.conditionType) {
            case REQUEST_TYPE -> matchesRequestType(request);
            case DEADLINE -> matchesDeadline(request);
            case IMPACT_LEVEL -> matchesImpactLevel(request);
            case REQUEST_TYPE_AND_DEADLINE -> matchesRequestType(request) && matchesDeadline(request);
        };
    }

    private boolean matchesRequestType(AcademicRequest request) {
        if (this.requestTypeId == null) {
            return true;
        }
        return this.requestTypeId.equals(request.getRequestTypeId());
    }

    private boolean matchesDeadline(AcademicRequest request) {
        if (request.getDeadline() == null) {
            return false;
        }
        try {
            var daysUntilDeadline = ChronoUnit.DAYS.between(LocalDate.now(), request.getDeadline());
            var thresholdDays = Long.parseLong(this.conditionValue);
            return daysUntilDeadline >= 0 && daysUntilDeadline <= thresholdDays;
        } catch (NumberFormatException e) {
            // If conditionValue is now complex JSON, legacy execution fails gracefully
            return false;
        }
    }

    private boolean matchesImpactLevel(AcademicRequest request) {
        if (this.conditionValue == null) return false;
        return this.conditionValue.equalsIgnoreCase(
                request.getPriority() != null ? request.getPriority().name() : ""
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BusinessRule that = (BusinessRule) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BusinessRule{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", conditionType=" + conditionType +
                ", resultingPriority=" + resultingPriority +
                ", active=" + active +
                '}';
    }
}
