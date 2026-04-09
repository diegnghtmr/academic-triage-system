package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.command.businessrule.DeactivateBusinessRuleCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveBusinessRulePort;
import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeactivateBusinessRuleServiceTest {

    @Mock
    private LoadBusinessRulePort loadBusinessRulePort;
    @Mock
    private SaveBusinessRulePort saveBusinessRulePort;

    @InjectMocks
    private DeactivateBusinessRuleService service;

    @Test
    @DisplayName("Should deactivate business rule successfully")
    void deactivateSuccessfully() {
        BusinessRuleId id = new BusinessRuleId(1L);
        BusinessRule existingRule = BusinessRule.reconstitute(
                id, "Rule", "Desc", ConditionType.DEADLINE, "10", Priority.LOW, null, true
        );

        DeactivateBusinessRuleCommand command = new DeactivateBusinessRuleCommand(id);

        when(loadBusinessRulePort.findById(id)).thenReturn(Optional.of(existingRule));

        service.deactivate(command);

        assertThat(existingRule.isActive()).isFalse();
        verify(saveBusinessRulePort).save(existingRule);
    }

    @Test
    @DisplayName("Should throw exception when rule not found for deactivation")
    void throwExceptionWhenNotFound() {
        BusinessRuleId id = new BusinessRuleId(1L);
        DeactivateBusinessRuleCommand command = new DeactivateBusinessRuleCommand(id);

        when(loadBusinessRulePort.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(command))
                .isInstanceOf(EntityNotFoundException.class);

        verify(saveBusinessRulePort, never()).save(any());
    }
}
