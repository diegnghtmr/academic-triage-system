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
        this.id = id;
        this.name = validateName(name);
        this.description = validateDescription(description);
        this.conditionType = Objects.requireNonNull(conditionType, "El conditionType no puede ser null");
        this.resultingPriority = Objects.requireNonNull(resultingPriority, "El resultingPriority no puede ser null");
        this.active = active;
        this.requestTypeId = requestTypeId;
        this.conditionValue = validateAndCanonConditionValue(conditionType, conditionValue, requestTypeId, active);
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
        this.resultingPriority = Objects.requireNonNull(resultingPriority, "El resultingPriority no puede ser null");
        this.active = active;
        this.requestTypeId = requestTypeId;
        this.conditionValue = validateAndCanonConditionValue(conditionType, conditionValue, requestTypeId, active);
    }

    private String validateAndCanonConditionValue(ConditionType conditionType, String conditionValue,
                                                    RequestTypeId requestTypeId, boolean active) {
        if (!active) {
            if (conditionValue == null || conditionValue.isBlank()) {
                return "0";
            }
            return conditionValue.trim();
        }
        if (conditionValue == null || conditionValue.isBlank()) {
            throw new IllegalArgumentException("El conditionValue no puede ser null o vacío");
        }
        var trimmed = conditionValue.trim();
        return switch (conditionType) {
            case REQUEST_TYPE -> canonRequestTypeValue(trimmed, requestTypeId);
            case DEADLINE -> canonDeadlineValue(trimmed, requestTypeId);
            case IMPACT_LEVEL -> canonImpactLevelValue(trimmed, requestTypeId);
            case REQUEST_TYPE_AND_DEADLINE -> canonRequestTypeAndDeadlineValue(trimmed, requestTypeId);
        };
    }

    private static String canonRequestTypeValue(String trimmed, RequestTypeId requestTypeId) {
        if (requestTypeId == null) {
            throw new IllegalArgumentException("REQUEST_TYPE requiere requestTypeId");
        }
        long parsed;
        try {
            parsed = Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("REQUEST_TYPE requiere conditionValue numérico (id del tipo de solicitud)");
        }
        if (parsed != requestTypeId.value()) {
            throw new IllegalArgumentException("conditionValue debe coincidir con el requestTypeId indicado");
        }
        return Long.toString(requestTypeId.value());
    }

    private static String canonDeadlineValue(String trimmed, RequestTypeId requestTypeId) {
        if (requestTypeId != null) {
            throw new IllegalArgumentException("DEADLINE no admite requestTypeId");
        }
        return Long.toString(parseNonNegativeDays(trimmed, "DEADLINE"));
    }

    private static String canonRequestTypeAndDeadlineValue(String trimmed, RequestTypeId requestTypeId) {
        if (requestTypeId == null) {
            throw new IllegalArgumentException("REQUEST_TYPE_AND_DEADLINE requiere requestTypeId");
        }
        return Long.toString(parseNonNegativeDays(trimmed, "REQUEST_TYPE_AND_DEADLINE"));
    }

    private static String canonImpactLevelValue(String trimmed, RequestTypeId requestTypeId) {
        if (requestTypeId != null) {
            throw new IllegalArgumentException("IMPACT_LEVEL no admite requestTypeId");
        }
        var upper = trimmed.toUpperCase();
        if (!upper.equals("HIGH") && !upper.equals("MEDIUM") && !upper.equals("LOW")) {
            throw new IllegalArgumentException("IMPACT_LEVEL debe ser HIGH, MEDIUM o LOW");
        }
        return upper;
    }

    private static long parseNonNegativeDays(String trimmed, String contextLabel) {
        try {
            var days = Long.parseLong(trimmed);
            if (days < 0) {
                throw new IllegalArgumentException(contextLabel + " requiere días no negativos");
            }
            return days;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(contextLabel + " requiere conditionValue numérico (días)");
        }
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
        this.conditionValue = validateAndCanonConditionValue(this.conditionType, this.conditionValue,
                this.requestTypeId, true);
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
        return this.requestTypeId != null && this.requestTypeId.equals(request.getRequestTypeId());
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
            return false;
        }
    }

    private boolean matchesImpactLevel(AcademicRequest request) {
        if (request.getPriority() == null) {
            return false;
        }
        return this.conditionValue.equals(request.getPriority().name());
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
