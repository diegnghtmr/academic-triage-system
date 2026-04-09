package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.businessrule.BusinessRuleView;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.ListBusinessRulesQuery;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListBusinessRulesServiceTest {

    @Mock
    private LoadBusinessRulePort loadBusinessRulePort;

    @Mock
    private BusinessRuleViewSupport businessRuleViewSupport;

    private ListBusinessRulesService service;

    @BeforeEach
    void setUp() {
        service = new ListBusinessRulesService(loadBusinessRulePort, businessRuleViewSupport);
        lenient().when(businessRuleViewSupport.hydrate(any(BusinessRule.class)))
                .thenAnswer(inv -> new BusinessRuleView(inv.getArgument(0), null));
    }

    @Test
    void shouldListBusinessRulesByFilter() {
        ListBusinessRulesQuery query = new ListBusinessRulesQuery(true, ConditionType.DEADLINE);
        List<BusinessRule> rules = List.of(
                BusinessRule.createNew("R1", "D1", ConditionType.DEADLINE, "10", Priority.LOW, null),
                BusinessRule.createNew("R2", "D2", ConditionType.DEADLINE, "20", Priority.MEDIUM, null)
        );

        when(loadBusinessRulePort.findAll(true, ConditionType.DEADLINE)).thenReturn(rules);

        List<BusinessRuleView> result = service.list(query);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).rule().getName()).isEqualTo("R1");
    }

    @Test
    void shouldListAllActiveRulesWhenNoConditionTypeProvided() {
        ListBusinessRulesQuery query = new ListBusinessRulesQuery(true, null);
        when(loadBusinessRulePort.findAll(true, null)).thenReturn(List.of());

        List<BusinessRuleView> result = service.list(query);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldListInactiveRules() {
        ListBusinessRulesQuery query = new ListBusinessRulesQuery(false, null);
        when(loadBusinessRulePort.findAll(false, null)).thenReturn(List.of());

        List<BusinessRuleView> result = service.list(query);

        assertThat(result).isEmpty();
    }
}
