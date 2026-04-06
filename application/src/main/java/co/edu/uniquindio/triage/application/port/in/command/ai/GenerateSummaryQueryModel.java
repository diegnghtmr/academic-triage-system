package co.edu.uniquindio.triage.application.port.in.command.ai;

import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.Objects;

public record GenerateSummaryQueryModel(RequestId requestId) {
    public GenerateSummaryQueryModel {
        Objects.requireNonNull(requestId, "El requestId no puede ser null");
    }
}
