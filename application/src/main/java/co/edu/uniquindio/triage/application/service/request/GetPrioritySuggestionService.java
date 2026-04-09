package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.PrioritySuggestionQuery;
import co.edu.uniquindio.triage.application.port.in.request.GetPrioritySuggestionQuery;
import co.edu.uniquindio.triage.application.port.in.request.MatchedBusinessRuleSummary;
import co.edu.uniquindio.triage.application.port.in.request.PrioritySuggestionResult;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.service.PriorityEngine;

import java.util.Objects;

public class GetPrioritySuggestionService implements GetPrioritySuggestionQuery {

    private final LoadRequestPort loadRequestPort;
    private final LoadBusinessRulePort loadBusinessRulePort;
    private final PriorityEngine priorityEngine;

    public GetPrioritySuggestionService(LoadRequestPort loadRequestPort,
                                        LoadBusinessRulePort loadBusinessRulePort,
                                        PriorityEngine priorityEngine) {
        this.loadRequestPort = Objects.requireNonNull(loadRequestPort, "loadRequestPort no puede ser null");
        this.loadBusinessRulePort = Objects.requireNonNull(loadBusinessRulePort, "loadBusinessRulePort no puede ser null");
        this.priorityEngine = Objects.requireNonNull(priorityEngine, "priorityEngine no puede ser null");
    }

    @Override
    public PrioritySuggestionResult execute(PrioritySuggestionQuery query, AuthenticatedActor actor) {
        Objects.requireNonNull(query, "query no puede ser null");
        Objects.requireNonNull(actor, "actor no puede ser null");

        var request = loadRequestPort.loadById(query.requestId())
                .orElseThrow(() -> new RequestNotFoundException(query.requestId()));

        if (actor.role() == Role.STUDENT && !request.getApplicantId().equals(actor.userId())) {
            throw new UnauthorizedOperationException(actor.role(), "sugerencia de prioridad");
        }

        var activeRules = loadBusinessRulePort.findAll(true, null);
        var evaluation = priorityEngine.evaluateWithDetails(request, activeRules);

        var summaries = evaluation.matchedRules().stream()
                .map(this::toSummary)
                .toList();

        return new PrioritySuggestionResult(evaluation.suggestedPriority(), summaries);
    }

    private MatchedBusinessRuleSummary toSummary(BusinessRule rule) {
        return new MatchedBusinessRuleSummary(
                Objects.requireNonNull(rule.getId(), "Regla matcheada debe tener id"),
                rule.getName(),
                rule.getResultingPriority()
        );
    }
}
