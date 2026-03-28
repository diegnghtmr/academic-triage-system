package co.edu.uniquindio.triage.infrastructure.adapter.in.rest;

import co.edu.uniquindio.triage.application.port.in.catalog.CreateOriginChannelUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.CreateRequestTypeUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.GetOriginChannelQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.GetRequestTypeQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.ListOriginChannelsQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.ListRequestTypesQuery;
import co.edu.uniquindio.triage.application.port.in.catalog.UpdateOriginChannelUseCase;
import co.edu.uniquindio.triage.application.port.in.catalog.UpdateRequestTypeUseCase;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.CreateOriginChannelRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.CreateRequestTypeRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.OriginChannelResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.RequestTypeResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.UpdateOriginChannelRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.UpdateRequestTypeRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper.CatalogRestMapper;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.support.AuthenticatedActorMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final CreateRequestTypeUseCase createRequestTypeUseCase;
    private final UpdateRequestTypeUseCase updateRequestTypeUseCase;
    private final ListOriginChannelsQuery listOriginChannelsQuery;
    private final GetOriginChannelQuery getOriginChannelQuery;
    private final CreateOriginChannelUseCase createOriginChannelUseCase;
    private final UpdateOriginChannelUseCase updateOriginChannelUseCase;
    private final CatalogRestMapper catalogRestMapper;
    private final AuthenticatedActorMapper authenticatedActorMapper;

    CatalogController(ListRequestTypesQuery listRequestTypesQuery,
                      GetRequestTypeQuery getRequestTypeQuery,
                      CreateRequestTypeUseCase createRequestTypeUseCase,
                      UpdateRequestTypeUseCase updateRequestTypeUseCase,
                      ListOriginChannelsQuery listOriginChannelsQuery,
                      GetOriginChannelQuery getOriginChannelQuery,
                      CreateOriginChannelUseCase createOriginChannelUseCase,
                      UpdateOriginChannelUseCase updateOriginChannelUseCase,
                      CatalogRestMapper catalogRestMapper,
                      AuthenticatedActorMapper authenticatedActorMapper) {
        this.listRequestTypesQuery = Objects.requireNonNull(listRequestTypesQuery);
        this.getRequestTypeQuery = Objects.requireNonNull(getRequestTypeQuery);
        this.createRequestTypeUseCase = Objects.requireNonNull(createRequestTypeUseCase);
        this.updateRequestTypeUseCase = Objects.requireNonNull(updateRequestTypeUseCase);
        this.listOriginChannelsQuery = Objects.requireNonNull(listOriginChannelsQuery);
        this.getOriginChannelQuery = Objects.requireNonNull(getOriginChannelQuery);
        this.createOriginChannelUseCase = Objects.requireNonNull(createOriginChannelUseCase);
        this.updateOriginChannelUseCase = Objects.requireNonNull(updateOriginChannelUseCase);
        this.catalogRestMapper = Objects.requireNonNull(catalogRestMapper);
        this.authenticatedActorMapper = Objects.requireNonNull(authenticatedActorMapper);
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
        return ResponseEntity.ok(catalogRestMapper.toResponse(result));
    }

    @PostMapping("/request-types")
    ResponseEntity<RequestTypeResponse> createRequestType(@Valid @RequestBody CreateRequestTypeRequest request,
                                                          Authentication authentication) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var created = createRequestTypeUseCase.execute(catalogRestMapper.toCommand(request), actor);
        var response = catalogRestMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/api/v1/catalogs/request-types/" + response.id())).body(response);
    }

    @PutMapping("/request-types/{typeId}")
    ResponseEntity<RequestTypeResponse> updateRequestType(@PathVariable("typeId") Long typeId,
                                                          @Valid @RequestBody UpdateRequestTypeRequest request,
                                                          Authentication authentication) {
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
        return ResponseEntity.ok(catalogRestMapper.toResponse(result));
    }

    @PostMapping("/origin-channels")
    ResponseEntity<OriginChannelResponse> createOriginChannel(@Valid @RequestBody CreateOriginChannelRequest request,
                                                              Authentication authentication) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var created = createOriginChannelUseCase.execute(catalogRestMapper.toCommand(request), actor);
        var response = catalogRestMapper.toResponse(created);
        return ResponseEntity.created(URI.create("/api/v1/catalogs/origin-channels/" + response.id())).body(response);
    }

    @PutMapping("/origin-channels/{channelId}")
    ResponseEntity<OriginChannelResponse> updateOriginChannel(@PathVariable("channelId") Long channelId,
                                                              @Valid @RequestBody UpdateOriginChannelRequest request,
                                                              Authentication authentication) {
        var actor = authenticatedActorMapper.toRequiredActor(authentication);
        var updated = updateOriginChannelUseCase.execute(catalogRestMapper.toCommand(channelId, request), actor);
        return ResponseEntity.ok(catalogRestMapper.toResponse(updated));
    }
}
