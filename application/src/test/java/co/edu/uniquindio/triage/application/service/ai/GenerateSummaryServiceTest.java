package co.edu.uniquindio.triage.application.service.ai;

import co.edu.uniquindio.triage.application.port.in.ai.AiGeneratedSummary;
import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.ai.GenerateSummaryQueryModel;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateSummaryServiceTest {

    @Mock
    private AiAssistantPort aiAssistantPort;

    @Mock
    private LoadRequestPort loadRequestPort;

    private GenerateSummaryService service;

    @BeforeEach
    void setUp() {
        service = new GenerateSummaryService(aiAssistantPort, loadRequestPort, new AiAuthorizationSupport());
    }

    @Test
    void execute_WhenAdmin_ShouldReturnSummary() {
        // Arrange
        var actor = new AuthenticatedActor(new UserId(1L), "admin", Role.ADMIN);
        var requestId = RequestId.of(100L);
        var query = new GenerateSummaryQueryModel(requestId);
        var detail = mock(RequestDetail.class);
        
        when(loadRequestPort.loadDetailById(requestId)).thenReturn(Optional.of(detail));
        
        var aiSummary = new AiGeneratedSummary(requestId, "Resumen de prueba", LocalDateTime.now());
        when(aiAssistantPort.generateSummary(detail)).thenReturn(aiSummary);

        // Act
        var result = service.execute(query, actor);

        // Assert
        assertThat(result.summary()).isEqualTo("Resumen de prueba");
    }

    @Test
    void execute_WhenRequestNotFound_ShouldThrowException() {
        // Arrange
        var actor = new AuthenticatedActor(new UserId(1L), "admin", Role.ADMIN);
        var requestId = RequestId.of(999L);
        var query = new GenerateSummaryQueryModel(requestId);
        
        when(loadRequestPort.loadDetailById(requestId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.execute(query, actor))
            .isInstanceOf(RequestNotFoundException.class);
    }
}
