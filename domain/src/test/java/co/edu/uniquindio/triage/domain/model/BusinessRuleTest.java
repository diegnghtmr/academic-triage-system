package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessRuleTest {

    @Test
    @DisplayName("Should create a new business rule successfully")
    void createNewSuccessfully() {
        BusinessRule rule = BusinessRule.createNew(
                "Rule Name",
                "Description",
                ConditionType.DEADLINE,
                "24",
                Priority.HIGH,
                new RequestTypeId(1L)
        );

        assertThat(rule.getName()).isEqualTo("Rule Name");
        assertThat(rule.getDescription()).isEqualTo("Description");
        assertThat(rule.getConditionType()).isEqualTo(ConditionType.DEADLINE);
        assertThat(rule.getConditionValue()).isEqualTo("24");
        assertThat(rule.getResultingPriority()).isEqualTo(Priority.HIGH);
        assertThat(rule.getRequestTypeId()).isEqualTo(new RequestTypeId(1L));
        assertThat(rule.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when name is invalid")
    void throwExceptionWhenNameInvalid() {
        assertThatThrownBy(() -> BusinessRule.createNew(null, "Desc", ConditionType.DEADLINE, "10", Priority.LOW, null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> BusinessRule.createNew("", "Desc", ConditionType.DEADLINE, "10", Priority.LOW, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception when name exceeds 150 characters")
    void throwExceptionWhenNameTooLong() {
        String longName = "a".repeat(151);
        assertThatThrownBy(() -> BusinessRule.createNew(longName, "Desc", ConditionType.DEADLINE, "10", Priority.LOW, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception when condition value is empty")
    void throwExceptionWhenConditionValueEmpty() {
        assertThatThrownBy(() -> BusinessRule.createNew("Name", "Desc", ConditionType.DEADLINE, "", Priority.LOW, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should deactivate rule")
    void deactivateRule() {
        BusinessRule rule = BusinessRule.createNew("Name", "Desc", ConditionType.DEADLINE, "10", Priority.LOW, null);
        assertThat(rule.isActive()).isTrue();

        rule.deactivate();
        assertThat(rule.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should update rule state")
    void updateRule() {
        BusinessRule rule = BusinessRule.createNew("Old Name", "Old Desc", ConditionType.DEADLINE, "10", Priority.LOW, null);

        rule.update(
                "New Name",
                "New Desc",
                ConditionType.REQUEST_TYPE,
                "new-val",
                Priority.HIGH,
                new RequestTypeId(999L),
                true
        );

        assertThat(rule.getName()).isEqualTo("New Name");
        assertThat(rule.getDescription()).isEqualTo("New Desc");
        assertThat(rule.getConditionType()).isEqualTo(ConditionType.REQUEST_TYPE);
        assertThat(rule.getConditionValue()).isEqualTo("new-val");
        assertThat(rule.getResultingPriority()).isEqualTo(Priority.HIGH);
        assertThat(rule.getRequestTypeId()).isEqualTo(new RequestTypeId(999L));
        assertThat(rule.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should match request type correctly")
    void matchRequestType() {
        RequestTypeId typeA = new RequestTypeId(1L);
        RequestTypeId typeB = new RequestTypeId(2L);
        BusinessRule rule = BusinessRule.createNew("Rule", "Desc", ConditionType.REQUEST_TYPE, "1", Priority.HIGH, typeA);
        
        AcademicRequest requestA = createRequest(typeA, null, null);
        AcademicRequest requestB = createRequest(typeB, null, null);

        assertThat(rule.matches(requestA)).isTrue();
        assertThat(rule.matches(requestB)).isFalse();
    }

    @Test
    @DisplayName("Should match deadline correctly")
    void matchDeadline() {
        BusinessRule rule = BusinessRule.createNew("Rule", "Desc", ConditionType.DEADLINE, "5", Priority.HIGH, null);
        
        AcademicRequest requestNear = createRequest(null, LocalDate.now().plusDays(3), null);
        AcademicRequest requestFar = createRequest(null, LocalDate.now().plusDays(10), null);
        AcademicRequest requestPast = createRequest(null, LocalDate.now().minusDays(1), null);

        assertThat(rule.matches(requestNear)).isTrue();
        assertThat(rule.matches(requestFar)).isFalse();
        assertThat(rule.matches(requestPast)).isFalse();
    }

    @Test
    @DisplayName("Should match impact level correctly")
    void matchImpactLevel() {
        BusinessRule rule = BusinessRule.createNew("Rule", "Desc", ConditionType.IMPACT_LEVEL, "HIGH", Priority.HIGH, null);
        
        AcademicRequest requestHigh = createRequest(null, null, Priority.HIGH);
        AcademicRequest requestLow = createRequest(null, null, Priority.LOW);

        assertThat(rule.matches(requestHigh)).isTrue();
        assertThat(rule.matches(requestLow)).isFalse();
    }

    @Test
    @DisplayName("Should match combined request type and deadline")
    void matchCombined() {
        RequestTypeId typeA = new RequestTypeId(1L);
        BusinessRule rule = BusinessRule.createNew("Rule", "Desc", ConditionType.REQUEST_TYPE_AND_DEADLINE, "5", Priority.HIGH, typeA);
        
        AcademicRequest requestMatch = createRequest(typeA, LocalDate.now().plusDays(3), null);
        AcademicRequest requestWrongType = createRequest(new RequestTypeId(2L), LocalDate.now().plusDays(3), null);
        AcademicRequest requestWrongDeadline = createRequest(typeA, LocalDate.now().plusDays(10), null);

        assertThat(rule.matches(requestMatch)).isTrue();
        assertThat(rule.matches(requestWrongType)).isFalse();
        assertThat(rule.matches(requestWrongDeadline)).isFalse();
    }

    @Test
    @DisplayName("Should return false for malformed deadline threshold")
    void malformedDeadlineThreshold() {
        BusinessRule rule = BusinessRule.reconstitute(
                new BusinessRuleId(1L), "Rule", "Desc",
                ConditionType.DEADLINE, "not-a-number", Priority.HIGH, null, true);
        
        AcademicRequest request = createRequest(null, LocalDate.now().plusDays(3), null);

        assertThat(rule.matches(request)).isFalse();
    }

    @Test
    @DisplayName("Should return false when request deadline is null")
    void nullRequestDeadline() {
        BusinessRule rule = BusinessRule.createNew("Rule", "Desc", ConditionType.DEADLINE, "5", Priority.HIGH, null);
        
        AcademicRequest request = createRequest(null, null, null);

        assertThat(rule.matches(request)).isFalse();
    }

    @Test
    @DisplayName("Should not match if rule is inactive")
    void noMatchIfInactive() {
        RequestTypeId typeA = new RequestTypeId(1L);
        BusinessRule rule = BusinessRule.createNew("Rule", "Desc", ConditionType.REQUEST_TYPE, "1", Priority.HIGH, typeA);
        rule.deactivate();
        
        AcademicRequest request = createRequest(typeA, null, null);

        assertThat(rule.matches(request)).isFalse();
    }

    private AcademicRequest createRequest(RequestTypeId typeId, LocalDate deadline, Priority priority) {
        AcademicRequest request = new AcademicRequest(
                co.edu.uniquindio.triage.domain.model.id.RequestId.of(1L),
                "Valid description",
                co.edu.uniquindio.triage.domain.model.id.UserId.of(1L),
                co.edu.uniquindio.triage.domain.model.id.OriginChannelId.of(1L),
                typeId != null ? typeId : new RequestTypeId(1L),
                deadline,
                false,
                java.time.LocalDateTime.now()
        );
        if (priority != null) {
            // Reconstitution or internal setter if available, but for now we need a way to set priority
            // Let's see if AcademicRequest has a way to set priority
            return AcademicRequest.reconstitute(
                request.getId(), request.getDescription(), co.edu.uniquindio.triage.domain.enums.RequestStatus.REGISTERED,
                priority, "Justification", deadline, request.getRegistrationDateTime(),
                false, null, null, null, null,
                request.getApplicantId(), null, request.getOriginChannelId(),
                request.getRequestTypeId(), null, null
            );
        }
        return request;
    }
}
