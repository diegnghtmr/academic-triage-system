package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.businessrule.BusinessRuleView;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBusinessRuleServiceTest {

    @Mock
    private LoadBusinessRulePort loadBusinessRulePort;

    @Mock
    private BusinessRuleViewSupport businessRuleViewSupport;

    @InjectMocks
    private GetBusinessRuleService service;

    @Test
    @DisplayName("Should return rule when found by ID")
    void returnRuleWhenFound() {
        BusinessRuleId id = new BusinessRuleId(1L);
        BusinessRule rule = BusinessRule.reconstitute(
                id, "Rule", "Desc", ConditionType.DEADLINE, "10", Priority.LOW, null, true);

        when(loadBusinessRulePort.findById(id)).thenReturn(Optional.of(rule));
        when(businessRuleViewSupport.hydrate(rule)).thenReturn(new BusinessRuleView(rule, null));

        Optional<BusinessRuleView> result = service.getById(id);

        assertThat(result).isPresent();
        assertThat(result.get().rule().getName()).isEqualTo("Rule");
    }

    @Test
    @DisplayName("Should return empty when rule not found by ID")
    void returnEmptyWhenNotFound() {
        BusinessRuleId id = new BusinessRuleId(1L);
        when(loadBusinessRulePort.findById(id)).thenReturn(Optional.empty());

        Optional<BusinessRuleView> result = service.getById(id);

        assertThat(result).isEmpty();
    }
}
