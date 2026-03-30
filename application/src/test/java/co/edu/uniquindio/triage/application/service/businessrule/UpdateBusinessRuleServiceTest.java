package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.command.businessrule.UpdateBusinessRuleCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveBusinessRulePort;
import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.id.BusinessRuleId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
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
class UpdateBusinessRuleServiceTest {

    @Mock
    private SaveBusinessRulePort saveBusinessRulePort;
    @Mock
    private LoadBusinessRulePort loadBusinessRulePort;
    @Mock
    private LoadRequestTypePort loadRequestTypePort;

    @InjectMocks
    private UpdateBusinessRuleService service;

    @Test
    @DisplayName("Should update business rule successfully")
    void updateSuccessfully() {
        BusinessRuleId id = new BusinessRuleId(1L);
        BusinessRule existingRule = new BusinessRule(
                id, "Old Name", "Old Desc", ConditionType.DEADLINE, "10", Priority.LOW, true, null
        );

        UpdateBusinessRuleCommand command = new UpdateBusinessRuleCommand(
                id, "New Name", "New Desc", ConditionType.REQUEST_TYPE, "new-val", Priority.HIGH, null, true
        );

        when(loadBusinessRulePort.findById(id)).thenReturn(Optional.of(existingRule));
        when(loadBusinessRulePort.existsByName("New Name")).thenReturn(false);
        when(saveBusinessRulePort.save(any(BusinessRule.class))).thenAnswer(i -> i.getArgument(0));

        BusinessRule result = service.update(command);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getResultingPriority()).isEqualTo(Priority.HIGH);
        verify(saveBusinessRulePort).save(any(BusinessRule.class));
    }

    @Test
    @DisplayName("Should throw exception when rule not found")
    void throwExceptionWhenNotFound() {
        BusinessRuleId id = new BusinessRuleId(1L);
        UpdateBusinessRuleCommand command = new UpdateBusinessRuleCommand(
                id, "Name", "Desc", ConditionType.DEADLINE, "10", Priority.HIGH, null, true
        );

        when(loadBusinessRulePort.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when name already exists for another rule")
    void throwExceptionWhenNameExists() {
        BusinessRuleId id = new BusinessRuleId(1L);
        BusinessRule existingRule = new BusinessRule(
                id, "Old Name", "Old Desc", ConditionType.DEADLINE, "10", Priority.LOW, true, null
        );

        UpdateBusinessRuleCommand command = new UpdateBusinessRuleCommand(
                id, "Duplicate", "Desc", ConditionType.DEADLINE, "10", Priority.HIGH, null, true
        );

        when(loadBusinessRulePort.findById(id)).thenReturn(Optional.of(existingRule));
        when(loadBusinessRulePort.existsByName("Duplicate")).thenReturn(true);

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(DuplicateCatalogEntryException.class);
    }

    @Test
    @DisplayName("Should throw exception when request type not found")
    void throwExceptionWhenRequestTypeNotFound() {
        BusinessRuleId id = new BusinessRuleId(1L);
        RequestTypeId rtId = new RequestTypeId(999L);
        BusinessRule existingRule = new BusinessRule(
                id, "Name", "Desc", ConditionType.DEADLINE, "10", Priority.LOW, true, null
        );
        UpdateBusinessRuleCommand command = new UpdateBusinessRuleCommand(
                id, "Name", "Desc", ConditionType.DEADLINE, "10", Priority.HIGH, rtId, true
        );

        when(loadBusinessRulePort.findById(id)).thenReturn(Optional.of(existingRule));
        when(loadRequestTypePort.loadById(rtId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
