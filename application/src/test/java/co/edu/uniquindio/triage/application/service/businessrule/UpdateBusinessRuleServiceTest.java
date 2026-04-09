package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.businessrule.BusinessRuleView;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.UpdateBusinessRuleCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadBusinessRulePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveBusinessRulePort;
import co.edu.uniquindio.triage.domain.enums.ConditionType;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.exception.DuplicateCatalogEntryException;
import co.edu.uniquindio.triage.domain.exception.EntityNotFoundException;
import co.edu.uniquindio.triage.domain.model.BusinessRule;
import co.edu.uniquindio.triage.domain.model.RequestType;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateBusinessRuleServiceTest {

    @Mock
    private SaveBusinessRulePort saveBusinessRulePort;
    @Mock
    private LoadBusinessRulePort loadBusinessRulePort;
    @Mock
    private LoadRequestTypePort loadRequestTypePort;
    @Mock
    private BusinessRuleViewSupport businessRuleViewSupport;

    @InjectMocks
    private UpdateBusinessRuleService service;

    @Test
    @DisplayName("Should update business rule successfully")
    void updateSuccessfully() {
        BusinessRuleId id = new BusinessRuleId(1L);
        RequestTypeId typeId = new RequestTypeId(5L);
        BusinessRule existingRule = BusinessRule.reconstitute(
                id, "Old Name", "Old Desc", ConditionType.DEADLINE, "10", Priority.LOW, null, true
        );

        UpdateBusinessRuleCommand command = new UpdateBusinessRuleCommand(
                id, "New Name", "New Desc", ConditionType.REQUEST_TYPE, "5", Priority.HIGH, typeId, true
        );

        when(loadBusinessRulePort.findById(id)).thenReturn(Optional.of(existingRule));
        when(loadBusinessRulePort.existsByName("New Name")).thenReturn(false);
        when(loadRequestTypePort.loadById(typeId)).thenReturn(Optional.of(mock(RequestType.class)));
        when(saveBusinessRulePort.save(any(BusinessRule.class))).thenAnswer(i -> i.getArgument(0));
        when(businessRuleViewSupport.hydrate(any())).thenAnswer(inv -> new BusinessRuleView(inv.getArgument(0), null));

        BusinessRuleView result = service.update(command);

        assertThat(result.rule().getName()).isEqualTo("New Name");
        assertThat(result.rule().getResultingPriority()).isEqualTo(Priority.HIGH);
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
        BusinessRule existingRule = BusinessRule.reconstitute(
                id, "Old Name", "Old Desc", ConditionType.DEADLINE, "10", Priority.LOW, null, true
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
        BusinessRule existingRule = BusinessRule.reconstitute(
                id, "Name", "Desc", ConditionType.DEADLINE, "10", Priority.LOW, null, true
        );
        UpdateBusinessRuleCommand command = new UpdateBusinessRuleCommand(
                id, "Name", "Desc", ConditionType.REQUEST_TYPE, "999", Priority.HIGH, rtId, true
        );

        when(loadBusinessRulePort.findById(id)).thenReturn(Optional.of(existingRule));
        when(loadRequestTypePort.loadById(rtId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
