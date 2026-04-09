package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.request.PrioritySuggestionQuery;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import co.edu.uniquindio.triage.domain.service.PriorityEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPrioritySuggestionServiceTest {

    @Mock
    private LoadRequestPort loadRequestPort;
    @Mock
    private LoadBusinessRulePort loadBusinessRulePort;

    private final PriorityEngine priorityEngine = new PriorityEngine();
    private GetPrioritySuggestionService service;

    @BeforeEach
    void setUp() {
        service = new GetPrioritySuggestionService(loadRequestPort, loadBusinessRulePort, priorityEngine);
    }

    @Test
    void returnsSuggestionAndMatchesWithoutPersisting() {
        var requestId = RequestId.of(1L);
        var typeId = new RequestTypeId(2L);
        var request = new AcademicRequest(
                requestId,
                "Descripción suficiente para validar la solicitud académica.",
                UserId.of(10L),
                OriginChannelId.of(1L),
                typeId,
                LocalDate.now().plusDays(2),
                false,
                LocalDateTime.now()
        );
        var rule = BusinessRule.reconstitute(
                new BusinessRuleId(9L), "R", "d",
                ConditionType.REQUEST_TYPE, "2", Priority.HIGH, typeId, true);

        when(loadRequestPort.loadById(requestId)).thenReturn(Optional.of(request));
        when(loadBusinessRulePort.findAll(true, null)).thenReturn(List.of(rule));

        var actor = new AuthenticatedActor(UserId.of(10L), "stu", Role.STUDENT);
        var result = service.execute(new PrioritySuggestionQuery(requestId), actor);

        assertThat(result.suggestedPriority()).isEqualTo(Priority.HIGH);
        assertThat(result.matchedRules()).hasSize(1);
        assertThat(result.matchedRules().get(0).ruleId()).isEqualTo(new BusinessRuleId(9L));
        assertThat(request.getPriority()).isNull();
    }

    @Test
    void returnsLowWhenNoRulesMatch() {
        var requestId = RequestId.of(1L);
        var request = new AcademicRequest(
                requestId,
                "Descripción suficiente para validar la solicitud académica.",
                UserId.of(10L),
                OriginChannelId.of(1L),
                new RequestTypeId(1L),
                LocalDate.now().plusDays(2),
                false,
                LocalDateTime.now()
        );
        var rule = BusinessRule.reconstitute(
                new BusinessRuleId(1L), "R", "d",
                ConditionType.REQUEST_TYPE, "99", Priority.HIGH, new RequestTypeId(99L), true);

        when(loadRequestPort.loadById(requestId)).thenReturn(Optional.of(request));
        when(loadBusinessRulePort.findAll(true, null)).thenReturn(List.of(rule));

        var result = service.execute(new PrioritySuggestionQuery(requestId),
                new AuthenticatedActor(UserId.of(1L), "staff", Role.STAFF));

        assertThat(result.suggestedPriority()).isEqualTo(Priority.LOW);
        assertThat(result.matchedRules()).isEmpty();
    }

    @Test
    void studentCannotSuggestForForeignRequest() {
        var requestId = RequestId.of(1L);
        var request = new AcademicRequest(
                requestId,
                "Descripción suficiente para validar la solicitud académica.",
                UserId.of(99L),
                OriginChannelId.of(1L),
                new RequestTypeId(1L),
                null,
                false,
                LocalDateTime.now()
        );
        when(loadRequestPort.loadById(requestId)).thenReturn(Optional.of(request));

        var actor = new AuthenticatedActor(UserId.of(10L), "stu", Role.STUDENT);

        assertThatThrownBy(() -> service.execute(new PrioritySuggestionQuery(requestId), actor))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void throwsWhenRequestMissing() {
        var requestId = RequestId.of(404L);
        when(loadRequestPort.loadById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new PrioritySuggestionQuery(requestId),
                new AuthenticatedActor(UserId.of(1L), "a", Role.ADMIN)))
                .isInstanceOf(RequestNotFoundException.class);
    }
}
