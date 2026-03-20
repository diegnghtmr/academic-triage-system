package co.edu.uniquindio.triage.infrastructure.adapter.persistence.entity;

import co.edu.uniquindio.triage.domain.enums.ConditionTypeEnum;
import co.edu.uniquindio.triage.domain.enums.PriorityEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "business_rules")
public class BusinessRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 100, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", length = 50, nullable = false)
    private ConditionTypeEnum conditionType;

    @Column(name = "condition_value", length = 255, nullable = false)
    private String conditionValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "resulting_priority", length = 20, nullable = false)
    private PriorityEnum resultingPriority;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_type_id")
    private RequestTypeEntity requestType;

    public BusinessRuleEntity() {
    }

    public BusinessRuleEntity(Long id, String name, String description,
                               ConditionTypeEnum conditionType, String conditionValue,
                               PriorityEnum resultingPriority, boolean active, RequestTypeEntity requestType) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.conditionType = conditionType;
        this.conditionValue = conditionValue;
        this.resultingPriority = resultingPriority;
        this.active = active;
        this.requestType = requestType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ConditionTypeEnum getConditionType() {
        return conditionType;
    }

    public void setConditionType(ConditionTypeEnum conditionType) {
        this.conditionType = conditionType;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(String conditionValue) {
        this.conditionValue = conditionValue;
    }

    public PriorityEnum getResultingPriority() {
        return resultingPriority;
    }

    public void setResultingPriority(PriorityEnum resultingPriority) {
        this.resultingPriority = resultingPriority;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public RequestTypeEntity getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestTypeEntity requestType) {
        this.requestType = requestType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BusinessRuleEntity that = (BusinessRuleEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "BusinessRuleEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", conditionType=" + conditionType +
                ", resultingPriority=" + resultingPriority +
                ", active=" + active +
                '}';
    }
}
