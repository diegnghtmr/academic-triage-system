package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.ConditionTypeEnum;
import co.edu.uniquindio.triage.domain.enums.PriorityEnum;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.util.Objects;

public class BusinessRule {
    private BusinessRuleId id;
    private String name;
    private String description;
    private ConditionTypeEnum conditionType;
    private String conditionValue;
    private PriorityEnum resultingPriority;
    private boolean active;
    private RequestTypeId requestTypeId;

    public BusinessRule(BusinessRuleId id, String name, String description,
                         ConditionTypeEnum conditionType, String conditionValue,
                         PriorityEnum resultingPriority, boolean active, RequestTypeId requestTypeId) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
        this.name = validateName(name);
        this.description = validateDescription(description);
        this.conditionType = Objects.requireNonNull(conditionType, "El conditionType no puede ser null");
        this.conditionValue = validateConditionValue(conditionValue);
        this.resultingPriority = Objects.requireNonNull(resultingPriority, "El resultingPriority no puede ser null");
        this.active = active;
        this.requestTypeId = requestTypeId;
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser null o vacío");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 100) {
            throw new IllegalArgumentException("El nombre no puede tener más de 100 caracteres");
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
        if (conditionValue.length() > 255) {
            throw new IllegalArgumentException("El conditionValue no puede tener más de 255 caracteres");
        }
        return conditionValue.trim();
    }

    public BusinessRuleId getId() {
        return id;
    }

    public void setId(BusinessRuleId id) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
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

    public ConditionTypeEnum getConditionType() {
        return conditionType;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public PriorityEnum getResultingPriority() {
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

    public void setRequestTypeId(RequestTypeId requestTypeId) {
        this.requestTypeId = requestTypeId;
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
