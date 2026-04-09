package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.businessrule.BusinessRuleView;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.CreateBusinessRuleCommand;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.ListBusinessRulesQuery;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.UpdateBusinessRuleCommand;
import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.CatalogRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule.BusinessRuleResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule.CreateBusinessRuleRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule.UpdateBusinessRuleRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
class BusinessRuleRestMapper {

    private final CatalogRestMapper requestTypeResponseMapper;

    public BusinessRuleRestMapper(CatalogRestMapper requestTypeResponseMapper) {
        this.requestTypeResponseMapper = Objects.requireNonNull(requestTypeResponseMapper, "requestTypeResponseMapper no puede ser null");
    }

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

    public List<BusinessRuleResponse> toResponses(List<BusinessRuleView> views) {
        Objects.requireNonNull(views, "La lista de vistas de reglas no puede ser null");
        return views.stream().map(this::toResponse).toList();
    }

    public BusinessRuleResponse toResponse(BusinessRuleView view) {
        Objects.requireNonNull(view, "La vista de regla no puede ser null");
        var rule = view.rule();
        var requestTypeResponse = view.requestType() != null ? requestTypeResponseMapper.toResponse(view.requestType()) : null;
        return new BusinessRuleResponse(
                rule.getId().value(),
                rule.getName(),
                rule.getDescription(),
                rule.getConditionType(),
                rule.getConditionValue(),
                rule.getResultingPriority(),
                requestTypeResponse,
                rule.isActive()
        );
    }
}
