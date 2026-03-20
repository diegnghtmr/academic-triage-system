package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestRuleId;

import java.util.Objects;

public class RequestRule {
    private RequestRuleId id;
    private BusinessRuleId ruleId;
    private RequestId requestId;

    public RequestRule(RequestRuleId id, BusinessRuleId ruleId, RequestId requestId) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
        this.ruleId = Objects.requireNonNull(ruleId, "El ruleId no puede ser null");
        this.requestId = Objects.requireNonNull(requestId, "El requestId no puede ser null");
    }

    public RequestRuleId getId() {
        return id;
    }

    public void setId(RequestRuleId id) {
        this.id = Objects.requireNonNull(id, "El id no puede ser null");
    }

    public BusinessRuleId getRuleId() {
        return ruleId;
    }

    public RequestId getRequestId() {
        return requestId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestRule that = (RequestRule) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "RequestRule{" +
                "id=" + id +
                ", ruleId=" + ruleId +
                ", requestId=" + requestId +
                '}';
    }
}
