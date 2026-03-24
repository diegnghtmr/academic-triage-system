package co.edu.uniquindio.triage.application.port.in.command.request;

import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.Objects;

public record GetRequestDetailQueryModel(RequestId requestId) {

    public GetRequestDetailQueryModel {
        Objects.requireNonNull(requestId, "El requestId no puede ser null");
    }
}
