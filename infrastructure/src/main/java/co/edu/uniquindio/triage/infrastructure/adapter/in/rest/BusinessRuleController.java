package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.businessrule.*;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.DeactivateBusinessRuleCommand;
import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule.BusinessRuleResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule.CreateBusinessRuleRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule.UpdateBusinessRuleRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.BusinessRuleRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/business-rules")
@RequiredArgsConstructor
@Tag(name = "Business Rules", description = "Endpoints para la administración de reglas de negocio")
public class BusinessRuleController {

    private final ListBusinessRulesQueryUseCase listBusinessRulesQueryUseCase;
    private final GetBusinessRuleQueryUseCase getBusinessRuleQueryUseCase;
    private final CreateBusinessRuleUseCase createBusinessRuleUseCase;
    private final UpdateBusinessRuleUseCase updateBusinessRuleUseCase;
    private final DeactivateBusinessRuleUseCase deactivateBusinessRuleUseCase;
    private final BusinessRuleRestMapper mapper;

    @GetMapping
    @Operation(summary = "Listar reglas de negocio", description = "Retorna una lista filtrada de reglas de negocio")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<BusinessRuleResponse>> listRules(
            @RequestParam(name = "active") Optional<Boolean> active,
            @RequestParam(name = "conditionType") Optional<ConditionType> conditionType
    ) {
        List<BusinessRule> result = listBusinessRulesQueryUseCase.list(mapper.toQuery(active, conditionType));
        return ResponseEntity.ok(mapper.toResponses(result));
    }

    @GetMapping("/{ruleId}")
    @Operation(summary = "Obtener regla por ID", description = "Retorna el detalle de una regla de negocio específica")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<BusinessRuleResponse> getById(@PathVariable("ruleId") Long ruleId) {
        BusinessRule result = getBusinessRuleQueryUseCase.getById(new BusinessRuleId(ruleId))
                .orElseThrow(() -> new EntityNotFoundException("Regla de negocio", "id", ruleId));
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    @PostMapping
    @Operation(summary = "Crear regla de negocio", description = "Crea una nueva regla de negocio (Solo ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusinessRuleResponse> create(@Valid @RequestBody CreateBusinessRuleRequest request) {
        BusinessRule result = createBusinessRuleUseCase.create(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(result));
    }

    @PutMapping("/{ruleId}")
    @Operation(summary = "Actualizar regla de negocio", description = "Actualiza una regla de negocio existente (Solo ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusinessRuleResponse> update(
            @PathVariable("ruleId") Long ruleId,
            @Valid @RequestBody UpdateBusinessRuleRequest request
    ) {
        BusinessRule result = updateBusinessRuleUseCase.update(mapper.toCommand(ruleId, request));
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    @DeleteMapping("/{ruleId}")
    @Operation(summary = "Desactivar regla de negocio", description = "Realiza una desactivación lógica de la regla (Solo ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("ruleId") Long ruleId) {
        deactivateBusinessRuleUseCase.deactivate(new DeactivateBusinessRuleCommand(new BusinessRuleId(ruleId)));
    }
}
