package co.edu.uniquindio.triage.application.port.in.command.catalog;

import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;

import java.util.Objects;

public record GetOriginChannelQueryModel(OriginChannelId originChannelId) {

    public GetOriginChannelQueryModel {
        Objects.requireNonNull(originChannelId, "El originChannelId no puede ser null");
    }
}
