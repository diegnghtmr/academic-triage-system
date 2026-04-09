package co.edu.uniquindio.triage.application.service.businessrule;

import co.edu.uniquindio.triage.application.port.in.businessrule.BusinessRuleView;
import co.edu.uniquindio.triage.application.port.in.command.businessrule.CreateBusinessRuleCommand;
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
class CreateBusinessRuleServiceTest {

    @Mock
    private SaveBusinessRulePort saveBusinessRulePort;
    @Mock
    private LoadBusinessRulePort loadBusinessRulePort;
    @Mock
    private LoadRequestTypePort loadRequestTypePort;
    @Mock
    private BusinessRuleViewSupport businessRuleViewSupport;

    @InjectMocks
    private CreateBusinessRuleService service;

    @Test
    @DisplayName("Should create business rule successfully")
    void createSuccessfully() {
        CreateBusinessRuleCommand command = new CreateBusinessRuleCommand(
                "Rule 1", "Desc", ConditionType.DEADLINE, "10", Priority.HIGH, null
        );
        when(loadBusinessRulePort.existsByName("Rule 1")).thenReturn(false);
        when(saveBusinessRulePort.save(any(BusinessRule.class))).thenAnswer(i -> {
            var r = i.getArgument(0, BusinessRule.class);
            return BusinessRule.reconstitute(
                    new BusinessRuleId(77L),
                    r.getName(),
                    r.getDescription(),
                    r.getConditionType(),
                    r.getConditionValue(),
                    r.getResultingPriority(),
                    r.getRequestTypeId(),
                    r.isActive());
        });
        when(businessRuleViewSupport.hydrate(any())).thenAnswer(inv -> new BusinessRuleView(inv.getArgument(0), null));

        BusinessRuleView result = service.create(command);

        assertThat(result.rule().getName()).isEqualTo("Rule 1");
        assertThat(result.rule().getId().value()).isEqualTo(77L);
        assertThat(result.rule().isActive()).isTrue();
        verify(saveBusinessRulePort).save(any(BusinessRule.class));
    }

    @Test
    @DisplayName("Should throw exception when name already exists")
    void throwExceptionWhenNameExists() {
        CreateBusinessRuleCommand command = new CreateBusinessRuleCommand(
                "Duplicate", "Desc", ConditionType.DEADLINE, "10", Priority.HIGH, null
        );
        when(loadBusinessRulePort.existsByName("Duplicate")).thenReturn(true);

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(DuplicateCatalogEntryException.class);

        verify(saveBusinessRulePort, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when request type not found")
    void throwExceptionWhenRequestTypeNotFound() {
        RequestTypeId rtId = new RequestTypeId(999L);
        CreateBusinessRuleCommand command = new CreateBusinessRuleCommand(
                "Rule 1", "Desc", ConditionType.REQUEST_TYPE, "999", Priority.HIGH, rtId
        );
        when(loadBusinessRulePort.existsByName("Rule 1")).thenReturn(false);
        when(loadRequestTypePort.loadById(rtId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
