package co.edu.uniquindio.triage.infrastructure.adapter.in.rest.mapper;

import co.edu.uniquindio.triage.application.port.in.command.catalog.CreateOriginChannelCommand;
import co.edu.uniquindio.triage.application.port.in.command.catalog.CreateRequestTypeCommand;
import co.edu.uniquindio.triage.application.port.in.command.catalog.GetOriginChannelQueryModel;
import co.edu.uniquindio.triage.application.port.in.command.catalog.GetRequestTypeQueryModel;
import co.edu.uniquindio.triage.application.port.in.command.catalog.ListOriginChannelsQueryModel;
import co.edu.uniquindio.triage.application.port.in.command.catalog.ListRequestTypesQueryModel;
import co.edu.uniquindio.triage.application.port.in.command.catalog.UpdateOriginChannelCommand;
import co.edu.uniquindio.triage.application.port.in.command.catalog.UpdateRequestTypeCommand;
import co.edu.uniquindio.triage.domain.model.OriginChannel;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.CreateOriginChannelRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.CreateRequestTypeRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.OriginChannelResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.RequestTypeResponse;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.UpdateOriginChannelRequest;
import co.edu.uniquindio.triage.infrastructure.adapter.in.rest.dto.catalog.UpdateRequestTypeRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class CatalogRestMapper {

    public ListRequestTypesQueryModel toRequestTypesQuery(Optional<Boolean> active) {
        return new ListRequestTypesQueryModel(active);
    }

    public GetRequestTypeQueryModel toRequestTypeQuery(Long requestTypeId) {
        return new GetRequestTypeQueryModel(new RequestTypeId(requestTypeId));
    }

    public CreateRequestTypeCommand toCommand(CreateRequestTypeRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new CreateRequestTypeCommand(request.name(), request.description());
    }

    public UpdateRequestTypeCommand toCommand(Long requestTypeId, UpdateRequestTypeRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new UpdateRequestTypeCommand(new RequestTypeId(requestTypeId), request.name(), request.description());
    }

    public List<RequestTypeResponse> toRequestTypeResponses(List<RequestType> requestTypes) {
        Objects.requireNonNull(requestTypes, "La lista de tipos de solicitud no puede ser null");
        return requestTypes.stream().map(this::toResponse).toList();
    }

    public RequestTypeResponse toResponse(RequestType requestType) {
        Objects.requireNonNull(requestType, "El tipo de solicitud no puede ser null");
        return new RequestTypeResponse(
                requestType.getId().value(),
                requestType.getName(),
                requestType.getDescription(),
                requestType.isActive()
        );
    }

    public ListOriginChannelsQueryModel toOriginChannelsQuery(Optional<Boolean> active) {
        return new ListOriginChannelsQueryModel(active);
    }

    public GetOriginChannelQueryModel toOriginChannelQuery(Long originChannelId) {
        return new GetOriginChannelQueryModel(new OriginChannelId(originChannelId));
    }

    public CreateOriginChannelCommand toCommand(CreateOriginChannelRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new CreateOriginChannelCommand(request.name());
    }

    public UpdateOriginChannelCommand toCommand(Long originChannelId, UpdateOriginChannelRequest request) {
        Objects.requireNonNull(request, "La solicitud HTTP no puede ser null");
        return new UpdateOriginChannelCommand(new OriginChannelId(originChannelId), request.name());
    }

    public List<OriginChannelResponse> toOriginChannelResponses(List<OriginChannel> originChannels) {
        Objects.requireNonNull(originChannels, "La lista de canales de origen no puede ser null");
        return originChannels.stream().map(this::toResponse).toList();
    }

    public OriginChannelResponse toResponse(OriginChannel originChannel) {
        Objects.requireNonNull(originChannel, "El canal de origen no puede ser null");
        return new OriginChannelResponse(
                originChannel.getId().value(),
                originChannel.getName(),
                originChannel.isActive()
        );
    }
}
