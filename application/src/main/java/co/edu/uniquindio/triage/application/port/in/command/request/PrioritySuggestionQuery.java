package co.edu.uniquindio.triage.application.port.in.command.request;

import co.edu.uniquindio.triage.domain.model.id.RequestId;

public record PrioritySuggestionQuery(RequestId requestId) {

    public PrioritySuggestionQuery {
        if (requestId == null) {
            throw new IllegalArgumentException("requestId no puede ser null");
        }
    }
}
