package co.edu.uniquindio.triage.application.port.in.command.catalog;

import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.util.Objects;

public record GetRequestTypeQueryModel(RequestTypeId requestTypeId) {

    public GetRequestTypeQueryModel {
        Objects.requireNonNull(requestTypeId, "El requestTypeId no puede ser null");
    }
}
