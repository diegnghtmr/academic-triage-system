package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.exception.ETagMismatchException;
import co.edu.uniquindio.triage.application.port.in.catalog.CreateOriginChannelUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.CreateRequestTypeUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.GetOriginChannelQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.GetOriginChannelVersionUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.GetRequestTypeQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.GetRequestTypeVersionUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.ListOriginChannelsQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.ListRequestTypesQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.UpdateOriginChannelUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.UpdateRequestTypeUseCase;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.CreateOriginChannelRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.CreateRequestTypeRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.OriginChannelResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.RequestTypeResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.UpdateOriginChannelRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.UpdateRequestTypeRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.CatalogRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.ETagSupport;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.HttpIdempotencySupport;
import co.edu.uniquindio.triage.infrastructure.idempotency.OperationScope;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/catalogs")
class CatalogController {

    private final ListRequestTypesQuery listRequestTypesQuery;
    private final GetRequestTypeQuery getRequestTypeQuery;
    private final GetRequestTypeVersionUseCase getRequestTypeVersionUseCase;
    private final CreateRequestTypeUseCase createRequestTypeUseCase;
    private final UpdateRequestTypeUseCase updateRequestTypeUseCase;
    private final ListOriginChannelsQuery listOriginChannelsQuery;
    private final GetOriginChannelQuery getOriginChannelQuery;
    private final GetOriginChannelVersionUseCase getOriginChannelVersionUseCase;
    private final CreateOriginChannelUseCase createOriginChannelUseCase;
    private final UpdateOriginChannelUseCase updateOriginChannelUseCase;
    private final CatalogRestMapper catalogRestMapper;
    private final AuthenticatedActorMapper authenticatedActorMapper;
    private final HttpIdempotencySupport httpIdempotencySupport;
    private final ETagSupport eTagSupport;

    CatalogController(ListRequestTypesQuery listRequestTypesQuery,
                      GetRequestTypeQuery getRequestTypeQuery,
                      GetRequestTypeVersionUseCase getRequestTypeVersionUseCase,
                      CreateRequestTypeUseCase createRequestTypeUseCase,
                      UpdateRequestTypeUseCase updateRequestTypeUseCase,
                      ListOriginChannelsQuery listOriginChannelsQuery,
                      GetOriginChannelQuery getOriginChannelQuery,
                      GetOriginChannelVersionUseCase getOriginChannelVersionUseCase,
                      CreateOriginChannelUseCase createOriginChannelUseCase,
                      UpdateOriginChannelUseCase updateOriginChannelUseCase,
                      CatalogRestMapper catalogRestMapper,
                      AuthenticatedActorMapper authenticatedActorMapper,
                      HttpIdempotencySupport httpIdempotencySupport,
                      ETagSupport eTagSupport) {
        this.listRequestTypesQuery = Objects.requireNonNull(listRequestTypesQuery);
        this.getRequestTypeQuery = Objects.requireNonNull(getRequestTypeQuery);
        this.getRequestTypeVersionUseCase = Objects.requireNonNull(getRequestTypeVersionUseCase);
        this.createRequestTypeUseCase = Objects.requireNonNull(createRequestTypeUseCase);
        this.updateRequestTypeUseCase = Objects.requireNonNull(updateRequestTypeUseCase);
        this.listOriginChannelsQuery = Objects.requireNonNull(listOriginChannelsQuery);
        this.getOriginChannelQuery = Objects.requireNonNull(getOriginChannelQuery);
        this.getOriginChannelVersionUseCase = Objects.requireNonNull(getOriginChannelVersionUseCase);
        this.createOriginChannelUseCase = Objects.requireNonNull(createOriginChannelUseCase);
        this.updateOriginChannelUseCase = Objects.requireNonNull(updateOriginChannelUseCase);
        this.catalogRestMapper = Objects.requireNonNull(catalogRestMapper);
        this.authenticatedActorMapper = Objects.requireNonNull(authenticatedActorMapper);
        this.httpIdempotencySupport = Objects.requireNonNull(httpIdempotencySupport);
        this.eTagSupport = Objects.requireNonNull(eTagSupport);
    }

    @GetMapping("/request-types")
    ResponseEntity<List<RequestTypeResponse>> listRequestTypes(@RequestParam(name = "active") Optional<Boolean> active,
                                                               Authentication authentication) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var result = listRequestTypesQuery.execute(catalogRestMapper.toRequestTypesQuery(active), actor);
        return ResponseEntity.ok(catalogRestMapper.toRequestTypeResponses(result));
    }

    @GetMapping("/request-types/{typeId}")
    ResponseEntity<RequestTypeResponse> getRequestTypeById(@PathVariable("typeId") Long typeId,
                                                           Authentication authentication) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var result = getRequestTypeQuery.execute(catalogRestMapper.toRequestTypeQuery(typeId), actor);
        var response = catalogRestMapper.toResponse(result);
        var version = getRequestTypeVersionUseCase.getVersionById(new RequestTypeId(typeId)).orElse(null);
        if (version != null) {
            return ResponseEntity.ok()
                    .eTag(eTagSupport.toETagValue(version))
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request-types")
    ResponseEntity<?> createRequestType(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateRequestTypeRequest request,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        return httpIdempotencySupport.execute(
                idempotencyKey,
                OperationScope.CATALOGS_REQUEST_TYPES_CREATE,
                "POST", "/api/v1/catalogs/request-types",
                String.valueOf(actor.userId().value()),
                MediaType.APPLICATION_JSON_VALUE,
                request,
                () -> {
                    var created = createRequestTypeUseCase.execute(catalogRestMapper.toCommand(request), actor);
                    var response = catalogRestMapper.toResponse(created);
                    return ResponseEntity.created(URI.create("/api/v1/catalogs/request-types/" + response.id())).body(response);
                }
        );
    }

    @PutMapping("/request-types/{typeId}")
    ResponseEntity<RequestTypeResponse> updateRequestType(
            @PathVariable("typeId") Long typeId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UpdateRequestTypeRequest request,
            Authentication authentication) {
        long expectedVersion = eTagSupport.parseIfMatch(ifMatch);
        long currentVersion = getRequestTypeVersionUseCase.getVersionById(new RequestTypeId(typeId))
                .orElseThrow(() -> new EntityNotFoundException("Tipo de solicitud", "id", typeId));
        if (expectedVersion != currentVersion) {
            throw new ETagMismatchException();
        }
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var updated = updateRequestTypeUseCase.execute(catalogRestMapper.toCommand(typeId, request), actor);
        return ResponseEntity.ok(catalogRestMapper.toResponse(updated));
    }

    @GetMapping("/origin-channels")
    ResponseEntity<List<OriginChannelResponse>> listOriginChannels(@RequestParam(name = "active") Optional<Boolean> active,
                                                                   Authentication authentication) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var result = listOriginChannelsQuery.execute(catalogRestMapper.toOriginChannelsQuery(active), actor);
        return ResponseEntity.ok(catalogRestMapper.toOriginChannelResponses(result));
    }

    @GetMapping("/origin-channels/{channelId}")
    ResponseEntity<OriginChannelResponse> getOriginChannelById(@PathVariable("channelId") Long channelId,
                                                               Authentication authentication) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var result = getOriginChannelQuery.execute(catalogRestMapper.toOriginChannelQuery(channelId), actor);
        var response = catalogRestMapper.toResponse(result);
        var version = getOriginChannelVersionUseCase.getVersionById(new OriginChannelId(channelId)).orElse(null);
        if (version != null) {
            return ResponseEntity.ok()
                    .eTag(eTagSupport.toETagValue(version))
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/origin-channels")
    ResponseEntity<?> createOriginChannel(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOriginChannelRequest request,
            Authentication authentication
    ) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        return httpIdempotencySupport.execute(
                idempotencyKey,
                OperationScope.CATALOGS_ORIGIN_CHANNELS_CREATE,
                "POST", "/api/v1/catalogs/origin-channels",
                String.valueOf(actor.userId().value()),
                MediaType.APPLICATION_JSON_VALUE,
                request,
                () -> {
                    var created = createOriginChannelUseCase.execute(catalogRestMapper.toCommand(request), actor);
                    var response = catalogRestMapper.toResponse(created);
                    return ResponseEntity.created(URI.create("/api/v1/catalogs/origin-channels/" + response.id())).body(response);
                }
        );
    }

    @PutMapping("/origin-channels/{channelId}")
    ResponseEntity<OriginChannelResponse> updateOriginChannel(
            @PathVariable("channelId") Long channelId,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UpdateOriginChannelRequest request,
            Authentication authentication) {
        long expectedVersion = eTagSupport.parseIfMatch(ifMatch);
        long currentVersion = getOriginChannelVersionUseCase.getVersionById(new OriginChannelId(channelId))
                .orElseThrow(() -> new EntityNotFoundException("Canal de origen", "id", channelId));
        if (expectedVersion != currentVersion) {
            throw new ETagMismatchException();
        }
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var updated = updateOriginChannelUseCase.execute(catalogRestMapper.toCommand(channelId, request), actor);
        return ResponseEntity.ok(catalogRestMapper.toResponse(updated));
    }
}
