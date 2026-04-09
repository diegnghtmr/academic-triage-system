package co.edu.uniquindio.triage.domain.service;

import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriorityEngineTest {

    private final PriorityEngine priorityEngine = new PriorityEngine();

    @Test
    @DisplayName("Should return HIGH when multiple rules match and one is HIGH")
    void shouldReturnHighestPriorityWhenMultipleMatch() {
        // Arrange
        RequestTypeId typeA = new RequestTypeId(1L);
        AcademicRequest request = createRequest(typeA);
        
        BusinessRule ruleLow = BusinessRule.reconstitute(
                new BusinessRuleId(1L), "Rule Low", "Desc",
                ConditionType.REQUEST_TYPE, "1", Priority.LOW, typeA, true);
        
        BusinessRule ruleHigh = BusinessRule.reconstitute(
                new BusinessRuleId(2L), "Rule High", "Desc",
                ConditionType.REQUEST_TYPE, "1", Priority.HIGH, typeA, true);
        
        List<BusinessRule> rules = Arrays.asList(ruleLow, ruleHigh);

        // Act
        Priority result = priorityEngine.evaluate(request, rules);

        // Assert
        assertEquals(Priority.HIGH, result);
    }

    @Test
    @DisplayName("Should return LOW when no rules match")
    void shouldReturnLowWhenNoRulesMatch() {
        // Arrange
        RequestTypeId typeA = new RequestTypeId(1L);
        RequestTypeId typeB = new RequestTypeId(2L);
        AcademicRequest request = createRequest(typeA);
        
        BusinessRule ruleMedium = BusinessRule.reconstitute(
                new BusinessRuleId(1L), "Rule Med", "Desc",
                ConditionType.REQUEST_TYPE, "2", Priority.MEDIUM, typeB, true);
        
        List<BusinessRule> rules = Collections.singletonList(ruleMedium);

        // Act
        Priority result = priorityEngine.evaluate(request, rules);

        // Assert
        assertEquals(Priority.LOW, result);
    }

    @Test
    @DisplayName("Should return LOW when rule list is empty")
    void shouldReturnLowWhenRuleListIsEmpty() {
        // Arrange
        AcademicRequest request = createRequest(new RequestTypeId(1L));

        // Act
        Priority result = priorityEngine.evaluate(request, Collections.emptyList());

        // Assert
        assertEquals(Priority.LOW, result);
    }

    @Test
    @DisplayName("Should throw NullPointerException when request is null")
    void shouldThrowExceptionWhenRequestIsNull() {
        assertThrows(NullPointerException.class, () -> priorityEngine.evaluate(null, Collections.emptyList()));
    }

    @Test
    @DisplayName("Should throw NullPointerException when rules list is null")
    void shouldThrowExceptionWhenRulesIsNull() {
        AcademicRequest request = createRequest(new RequestTypeId(1L));
        assertThrows(NullPointerException.class, () -> priorityEngine.evaluate(request, null));
    }

    @Test
    @DisplayName("Should return detailed evaluation with matched rules ordered by inclusion")
    void shouldReturnDetailedEvaluationWithMatches() {
        RequestTypeId typeA = new RequestTypeId(1L);
        AcademicRequest request = createRequest(typeA);

        BusinessRule ruleLow = BusinessRule.reconstitute(
                new BusinessRuleId(1L), "Rule Low", "Desc",
                ConditionType.REQUEST_TYPE, "1", Priority.LOW, typeA, true);

        BusinessRule ruleHigh = BusinessRule.reconstitute(
                new BusinessRuleId(2L), "Rule High", "Desc",
                ConditionType.REQUEST_TYPE, "1", Priority.HIGH, typeA, true);

        var details = priorityEngine.evaluateWithDetails(request, List.of(ruleLow, ruleHigh));

        assertThat(details.suggestedPriority()).isEqualTo(Priority.HIGH);
        assertThat(details.matchedRules()).hasSize(2);
        assertThat(details.matchedRules()).extracting(BusinessRule::getId).containsExactlyInAnyOrder(
                new BusinessRuleId(1L), new BusinessRuleId(2L));
    }

    @Test
    @DisplayName("Should return LOW and empty matches when nothing matches")
    void shouldReturnLowWithEmptyMatches() {
        AcademicRequest request = createRequest(new RequestTypeId(1L));
        var details = priorityEngine.evaluateWithDetails(request, Collections.emptyList());
        assertThat(details.suggestedPriority()).isEqualTo(Priority.LOW);
        assertThat(details.matchedRules()).isEmpty();
    }

    private AcademicRequest createRequest(RequestTypeId typeId) {
        return new AcademicRequest(
                RequestId.of(1L),
                "Test description",
                new UserId(1L),
                new OriginChannelId(1L),
                typeId,
                LocalDate.now().plusDays(5),
                false,
                LocalDateTime.now()
        );
    }
}
