package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.exception.ETagMismatchException;
import co.edu.uniquindio.triage.application.port.in.auth.ActorPrincipal;
import co.edu.uniquindio.triage.application.port.in.businessrule.*;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.DeactivateBusinessRuleCommand;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule.BusinessRuleResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule.CreateBusinessRuleRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.businessrule.UpdateBusinessRuleRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.ETagSupport;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.HttpIdempotencySupport;
import co.edu.uniquindio.triage.infrastructure.idempotency.OperationScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/business-rules")
@RequiredArgsConstructor
@Tag(name = "Business Rules", description = "Endpoints para la administración de reglas de negocio")
class BusinessRuleController {

    private final ListBusinessRulesQueryUseCase listBusinessRulesQueryUseCase;
    private final GetBusinessRuleQueryUseCase getBusinessRuleQueryUseCase;
    private final GetBusinessRuleVersionUseCase getBusinessRuleVersionUseCase;
    private final CreateBusinessRuleUseCase createBusinessRuleUseCase;
    private final UpdateBusinessRuleUseCase updateBusinessRuleUseCase;
    private final DeactivateBusinessRuleUseCase deactivateBusinessRuleUseCase;
    private final BusinessRuleRestMapper mapper;
    private final ETagSupport eTagSupport;
    private final HttpIdempotencySupport httpIdempotencySupport;

    @GetMapping
    @Operation(summary = "Listar reglas de negocio", description = "Retorna una lista filtrada de reglas de negocio")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<BusinessRuleResponse>> listRules(
            @RequestParam(name = "active") Optional<Boolean> active,
            @RequestParam(name = "conditionType") Optional<String> conditionType
    ) {
        var result = listBusinessRulesQueryUseCase.list(mapper.toQuery(active, conditionType));
        return ResponseEntity.ok(mapper.toResponses(result));
    }

    @GetMapping("/{ruleId}")
    @Operation(
            summary = "Obtener regla por ID",
            description = "Retorna el detalle de una regla de negocio. Emite ETag con la versión actual para uso en If-Match.",
            responses = @ApiResponse(
                    responseCode = "200",
                    headers = @Header(name = "ETag", description = "Versión del recurso", schema = @Schema(type = "string", example = "\"7\""))
            )
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<BusinessRuleResponse> getById(@PathVariable("ruleId") Long ruleId) {
        var view = getBusinessRuleQueryUseCase.getById(new BusinessRuleId(ruleId))
                .orElseThrow(() -> new EntityNotFoundException("Regla de negocio", "id", ruleId));
        var response = mapper.toResponse(view);
        var version = getBusinessRuleVersionUseCase.getVersionById(new BusinessRuleId(ruleId)).orElse(null);
        if (version != null) {
            return ResponseEntity.ok()
                    .eTag(eTagSupport.toETagValue(version))
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Operation(
            summary = "Crear regla de negocio",
            description = "Crea una nueva regla de negocio. Requiere Idempotency-Key (Solo ADMIN).",
            responses = {
                    @ApiResponse(responseCode = "201", headers = @Header(name = "Idempotency-Status", description = "fresh | replayed", schema = @Schema(type = "string"))),
                    @ApiResponse(responseCode = "400", description = "Falta Idempotency-Key o payload inválido"),
                    @ApiResponse(responseCode = "409", description = "Solicitud en procesamiento para la misma clave"),
                    @ApiResponse(responseCode = "422", description = "Fingerprint distinto para la misma Idempotency-Key")
            }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(
            @Parameter(description = "Clave de idempotencia única por operación", required = true)
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateBusinessRuleRequest request,
            Authentication authentication
    ) {
        var principalScope = extractPrincipalScope(authentication);
        return httpIdempotencySupport.execute(
                idempotencyKey,
                OperationScope.BUSINESS_RULES_CREATE,
                "POST", "/api/v1/business-rules",
                principalScope,
                MediaType.APPLICATION_JSON_VALUE,
                request,
                () -> {
                    var result = createBusinessRuleUseCase.create(mapper.toCommand(request));
                    return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(result));
                }
        );
    }

    @PutMapping("/{ruleId}")
    @Operation(
            summary = "Actualizar regla de negocio",
            description = "Actualiza una regla de negocio existente. Requiere If-Match con la versión obtenida del GET (Solo ADMIN).",
            responses = {
                    @ApiResponse(responseCode = "200"),
                    @ApiResponse(responseCode = "412", description = "ETag no coincide con la versión actual del recurso"),
                    @ApiResponse(responseCode = "428", description = "Falta header If-Match")
            }
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BusinessRuleResponse> update(
            @PathVariable("ruleId") Long ruleId,
            @Parameter(description = "Versión del recurso obtenida del ETag del GET", required = true, example = "\"7\"")
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UpdateBusinessRuleRequest request
    ) {
        long expectedVersion = eTagSupport.parseIfMatch(ifMatch);
        long currentVersion = getBusinessRuleVersionUseCase.getVersionById(new BusinessRuleId(ruleId))
                .orElseThrow(() -> new EntityNotFoundException("Regla de negocio", "id", ruleId));
        if (expectedVersion != currentVersion) {
            throw new ETagMismatchException();
        }
        var result = updateBusinessRuleUseCase.update(mapper.toCommand(ruleId, request));
        return ResponseEntity.ok(mapper.toResponse(result));
    }

    @DeleteMapping("/{ruleId}")
    @Operation(
            summary = "Desactivar regla de negocio",
            description = "Desactivación lógica del recurso. Requiere If-Match con la versión obtenida del GET (Solo ADMIN).",
            responses = {
                    @ApiResponse(responseCode = "204"),
                    @ApiResponse(responseCode = "412", description = "ETag no coincide con la versión actual del recurso"),
                    @ApiResponse(responseCode = "428", description = "Falta header If-Match")
            }
    )
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable("ruleId") Long ruleId,
            @Parameter(description = "Versión del recurso obtenida del ETag del GET", required = true, example = "\"7\"")
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = eTagSupport.parseIfMatch(ifMatch);
        long currentVersion = getBusinessRuleVersionUseCase.getVersionById(new BusinessRuleId(ruleId))
                .orElseThrow(() -> new EntityNotFoundException("Regla de negocio", "id", ruleId));
        if (expectedVersion != currentVersion) {
            throw new ETagMismatchException();
        }
        deactivateBusinessRuleUseCase.deactivate(new DeactivateBusinessRuleCommand(new BusinessRuleId(ruleId)));
    }

    private String extractPrincipalScope(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof ActorPrincipal ap) {
            return String.valueOf(ap.id());
        }
        return "";
    }
}
