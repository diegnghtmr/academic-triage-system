package co.edu.uniquindio.triage.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

@Entity
@Table(name = "request_rules",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_request_rules_rule_request",
                        columnNames = {"rule_id", "request_id"})
        },
        indexes = {
                @Index(name = "idx_request_rules_rule", columnList = "rule_id"),
                @Index(name = "idx_request_rules_request", columnList = "request_id")
        }
)
public class RequestRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private BusinessRuleEntity rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private AcademicRequestEntity request;

    public RequestRuleEntity() {
    }

    public RequestRuleEntity(Long id, BusinessRuleEntity rule, AcademicRequestEntity request) {
        this.id = id;
        this.rule = rule;
        this.request = request;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BusinessRuleEntity getRule() {
        return rule;
    }

    public void setRule(BusinessRuleEntity rule) {
        this.rule = rule;
    }

    public AcademicRequestEntity getRequest() {
        return request;
    }

    public void setRequest(AcademicRequestEntity request) {
        this.request = request;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestRuleEntity that = (RequestRuleEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RequestRuleEntity{" +
                "id=" + id +
                ", ruleId=" + (rule != null ? rule.getId() : null) +
                ", requestId=" + (request != null ? request.getId() : null) +
                '}';
    }
}
