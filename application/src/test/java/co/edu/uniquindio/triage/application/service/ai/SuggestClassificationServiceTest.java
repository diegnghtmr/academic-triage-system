package co.edu.uniquindio.triage.application.service.ai;

import co.edu.uniquindio.triage.application.port.in.ai.AiClassificationSuggestion;
import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.ai.SuggestClassificationCommand;
import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestTypePort;
import co.edu.uniquindio.triage.domain.enums.Priority;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuggestClassificationServiceTest {

    @Mock
    private AiAssistantPort aiAssistantPort;

    @Mock
    private LoadRequestTypePort loadRequestTypePort;

    private SuggestClassificationService service;

    @BeforeEach
    void setUp() {
        service = new SuggestClassificationService(aiAssistantPort, loadRequestTypePort, new AiAuthorizationSupport());
    }

    @Test
    void execute_WhenStaff_ShouldReturnSuggestionWithResolvedId() {
        // Arrange
        var actor = new AuthenticatedActor(new UserId(1L), "staff", Role.STAFF);
        var command = new SuggestClassificationCommand("Descripción de prueba");
        var requestTypeId = new RequestTypeId(10L);
        var requestType = new RequestType(requestTypeId, "Cupo", "Descripción cupo", true);
        
        when(loadRequestTypePort.loadAllRequestTypes(any())).thenReturn(List.of(requestType));
        
        var aiSuggestion = new AiClassificationSuggestion("Cupo", Optional.empty(), Priority.HIGH, 0.9, "Razonamiento");
        when(aiAssistantPort.suggestClassification(eq(command.description()), any())).thenReturn(aiSuggestion);

        // Act
        var result = service.execute(command, actor);

        // Assert
        assertThat(result.requestTypeName()).isEqualTo("Cupo");
        assertThat(result.suggestedRequestTypeId()).contains(requestTypeId);
        assertThat(result.suggestedPriority()).isEqualTo(Priority.HIGH);
        assertThat(result.confidence()).isEqualTo(0.9);
    }

    @Test
    void execute_WhenNoActiveMatch_ShouldReturnEmptyId() {
        // Arrange
        var actor = new AuthenticatedActor(new UserId(1L), "staff", Role.STAFF);
        var command = new SuggestClassificationCommand("Descripción de prueba");
        
        when(loadRequestTypePort.loadAllRequestTypes(any())).thenReturn(List.of());
        
        var aiSuggestion = new AiClassificationSuggestion("Inexistente", Optional.empty(), Priority.LOW, 0.5, "Razonamiento");
        when(aiAssistantPort.suggestClassification(eq(command.description()), any())).thenReturn(aiSuggestion);

        // Act
        var result = service.execute(command, actor);

        // Assert
        assertThat(result.suggestedRequestTypeId()).isEmpty();
    }
}
