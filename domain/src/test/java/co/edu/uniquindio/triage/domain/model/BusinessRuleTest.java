package co.edu.uniquindio.triage.domain.model;

import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
