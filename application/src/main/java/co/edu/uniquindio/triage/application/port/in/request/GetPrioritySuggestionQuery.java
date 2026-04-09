package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.PrioritySuggestionQuery;

public interface GetPrioritySuggestionQuery {

    PrioritySuggestionResult execute(PrioritySuggestionQuery query, AuthenticatedActor actor);
}
