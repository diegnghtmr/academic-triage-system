package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper;

import co.edu.uniquindio.triage.application.port.in.command.businessrule.CreateBusinessRuleCommand;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.ListBusinessRulesQuery;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.UpdateBusinessRuleCommand;
import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule.BusinessRuleResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule.CreateBusinessRuleRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule.UpdateBusinessRuleRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.RequestTypeResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class BusinessRuleRestMapper {

    public ListBusinessRulesQuery toQuery(Optional<Boolean> active, Optional<ConditionType> conditionType) {
        return new ListBusinessRulesQuery(active.orElse(null), conditionType.orElse(null));
    }

    public CreateBusinessRuleCommand toCommand(CreateBusinessRuleRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new CreateBusinessRuleCommand(
                request.name(),
                request.description(),
                request.conditionType(),
                request.conditionValue(),
                request.resultingPriority(),
                request.requestTypeId() != null ? new RequestTypeId(request.requestTypeId()) : null
        );
    }

    public UpdateBusinessRuleCommand toCommand(Long ruleId, UpdateBusinessRuleRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new UpdateBusinessRuleCommand(
                new BusinessRuleId(ruleId),
                request.name(),
                request.description(),
                request.conditionType(),
                request.conditionValue(),
                request.resultingPriority(),
                request.requestTypeId() != null ? new RequestTypeId(request.requestTypeId()) : null,
                request.active()
        );
    }

    public List<BusinessRuleResponse> toResponses(List<BusinessRule> rules) {
        Objects.requireNonNull(rules, "La lista de reglas de negocio no puede ser null");
        return rules.stream().map(this::toResponse).toList();
    }

    public BusinessRuleResponse toResponse(BusinessRule rule) {
        Objects.requireNonNull(rule, "La regla de negocio no puede ser null");
        return new BusinessRuleResponse(
                rule.getId().value(),
                rule.getName(),
                rule.getDescription(),
                rule.getConditionType(),
                rule.getConditionValue(),
                rule.getResultingPriority(),
                rule.getRequestTypeId() != null ? new RequestTypeResponse(rule.getRequestTypeId().value(), null, null, true) : null,
                rule.isActive()
        );
    }
}
