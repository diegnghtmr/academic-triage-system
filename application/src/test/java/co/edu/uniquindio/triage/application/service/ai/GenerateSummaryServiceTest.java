package co.edu.uniquindio.triage.application.service.ai;

import co.edu.uniquindio.triage.application.port.in.ai.AiGeneratedSummary;
import co.edu.uniquindio.triage.application.port.in.auth.AuthenticatedActor;
import co.edu.uniquindio.triage.application.port.in.command.ai.GenerateSummaryQueryModel;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.out.ai.AiAssistantPort;
import co.edu.uniquindio.triage.application.port.out.ai.LoadAiSummaryCachePort;
import co.edu.uniquindio.triage.application.port.out.ai.SaveAiSummaryCachePort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestVersionPort;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.exception.UnauthorizedOperationException;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateSummaryServiceTest {

    @Mock
    private AiAssistantPort aiAssistantPort;
    @Mock
    private LoadRequestPort loadRequestPort;
    @Mock
    private LoadRequestVersionPort loadRequestVersionPort;
    @Mock
    private LoadAiSummaryCachePort loadAiSummaryCachePort;
    @Mock
    private SaveAiSummaryCachePort saveAiSummaryCachePort;

    private GenerateSummaryService service;

    private static final RequestId REQUEST_ID = RequestId.of(100L);
    private static final long VERSION = 3L;
    private static final AuthenticatedActor STAFF_ACTOR =
            new AuthenticatedActor(new UserId(1L), "staffUser", Role.STAFF);
    private static final AuthenticatedActor ADMIN_ACTOR =
            new AuthenticatedActor(new UserId(2L), "adminUser", Role.ADMIN);
    private static final AuthenticatedActor STUDENT_ACTOR =
            new AuthenticatedActor(new UserId(3L), "student", Role.STUDENT);

    @BeforeEach
    void setUp() {
        service = new GenerateSummaryService(
                aiAssistantPort,
                loadRequestPort,
                loadRequestVersionPort,
                loadAiSummaryCachePort,
                saveAiSummaryCachePort,
                new AiAuthorizationSupport()
        );
    }

    // ─── Caso 1: cache hit ──────────────────────────────────────────────────

    @Test
    void execute_WhenCacheHit_ShouldReturnCachedSummaryWithoutCallingAI() {
        var query = new GenerateSummaryQueryModel(REQUEST_ID);
        var cached = new AiGeneratedSummary(REQUEST_ID, "Resumen cacheado", LocalDateTime.now());

        when(loadRequestVersionPort.findVersionById(REQUEST_ID)).thenReturn(Optional.of(VERSION));
        when(loadAiSummaryCachePort.findByRequestIdAndVersion(REQUEST_ID, VERSION)).thenReturn(Optional.of(cached));

        var result = service.execute(query, STAFF_ACTOR);

        assertThat(result.summary()).isEqualTo("Resumen cacheado");
        verifyNoInteractions(aiAssistantPort);
        verify(saveAiSummaryCachePort, never()).save(any(), anyLong(), any());
    }

    // ─── Caso 2: cache miss ─────────────────────────────────────────────────

    @Test
    void execute_WhenCacheMiss_ShouldCallAISaveAndReturn() {
        var query = new GenerateSummaryQueryModel(REQUEST_ID);
        var detail = mock(RequestDetail.class);
        var fresh = new AiGeneratedSummary(REQUEST_ID, "Resumen fresco", LocalDateTime.now());

        when(loadRequestVersionPort.findVersionById(REQUEST_ID)).thenReturn(Optional.of(VERSION));
        when(loadAiSummaryCachePort.findByRequestIdAndVersion(REQUEST_ID, VERSION)).thenReturn(Optional.empty());
        when(loadRequestPort.loadDetailById(REQUEST_ID)).thenReturn(Optional.of(detail));
        when(aiAssistantPort.generateSummary(detail)).thenReturn(fresh);

        var result = service.execute(query, ADMIN_ACTOR);

        assertThat(result.summary()).isEqualTo("Resumen fresco");
        verify(aiAssistantPort).generateSummary(detail);
        verify(saveAiSummaryCachePort).save(REQUEST_ID, VERSION, fresh);
    }

    // ─── Caso 3: version no encontrada ─────────────────────────────────────

    @Test
    void execute_WhenVersionNotFound_ShouldThrowRequestNotFoundException() {
        var query = new GenerateSummaryQueryModel(REQUEST_ID);

        when(loadRequestVersionPort.findVersionById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(query, STAFF_ACTOR))
                .isInstanceOf(RequestNotFoundException.class);

        verifyNoInteractions(aiAssistantPort, loadAiSummaryCachePort, saveAiSummaryCachePort);
    }

    // ─── Caso 4: version existe pero detail faltante ────────────────────────

    @Test
    void execute_WhenVersionExistsButDetailMissing_ShouldThrowRequestNotFoundException() {
        var query = new GenerateSummaryQueryModel(REQUEST_ID);

        when(loadRequestVersionPort.findVersionById(REQUEST_ID)).thenReturn(Optional.of(VERSION));
        when(loadAiSummaryCachePort.findByRequestIdAndVersion(REQUEST_ID, VERSION)).thenReturn(Optional.empty());
        when(loadRequestPort.loadDetailById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(query, STAFF_ACTOR))
                .isInstanceOf(RequestNotFoundException.class);

        verifyNoInteractions(aiAssistantPort);
        verify(saveAiSummaryCachePort, never()).save(any(), anyLong(), any());
    }

    // ─── Caso 5: autorización ───────────────────────────────────────────────

    @Test
    void execute_WhenActorIsStudent_ShouldThrowUnauthorizedOperationException() {
        var query = new GenerateSummaryQueryModel(REQUEST_ID);

        assertThatThrownBy(() -> service.execute(query, STUDENT_ACTOR))
                .isInstanceOf(UnauthorizedOperationException.class);

        verifyNoInteractions(loadRequestVersionPort, loadAiSummaryCachePort, aiAssistantPort, saveAiSummaryCachePort);
    }
}
