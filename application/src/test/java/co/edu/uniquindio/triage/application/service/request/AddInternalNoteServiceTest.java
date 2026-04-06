package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.command.request.AddInternalNoteCommand;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort;
import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddInternalNoteServiceTest {

    @Mock
    private LoadRequestPort loadRequestPort;

    @Mock
    private SaveRequestPort saveRequestPort;

    private AddInternalNoteService service;

    @BeforeEach
    void setUp() {
        service = new AddInternalNoteService(loadRequestPort, saveRequestPort);
    }

    @Test
    void shouldAddInternalNoteSuccessfully() {
        // GIVEN
        var requestId = new RequestId(1L);
        var performedById = new UserId(2L);
        var note = "This is an internal note";
        var command = new AddInternalNoteCommand(requestId, note, performedById);

        var request = createSampleRequest(requestId);
        when(loadRequestPort.loadById(requestId)).thenReturn(Optional.of(request));

        // WHEN
        service.addInternalNote(command);

        // THEN
        var requestCaptor = ArgumentCaptor.forClass(AcademicRequest.class);
        verify(saveRequestPort).save(requestCaptor.capture());

        var savedRequest = requestCaptor.getValue();
        assertThat(savedRequest.getHistory()).hasSize(2); // Initial REGISTERED + INTERNAL_NOTE
        var lastHistory = savedRequest.getHistory().getLast();
        assertThat(lastHistory.getAction()).isEqualTo(HistoryAction.INTERNAL_NOTE);
        assertThat(lastHistory.getObservations()).isEqualTo(note);
        assertThat(lastHistory.getPerformedById()).isEqualTo(performedById);
    }

    @Test
    void shouldThrowExceptionWhenRequestNotFound() {
        // GIVEN
        var requestId = new RequestId(1L);
        var command = new AddInternalNoteCommand(requestId, "Note", new UserId(2L));
        when(loadRequestPort.loadById(requestId)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> service.addInternalNote(command))
                .isInstanceOf(RequestNotFoundException.class);

        verify(saveRequestPort, never()).save(any());
    }

    private AcademicRequest createSampleRequest(RequestId id) {
        return new AcademicRequest(
                id,
                "Sample request description with enough length",
                new UserId(10L),
                new OriginChannelId(1L),
                new RequestTypeId(1L),
                null,
                false,
                LocalDateTime.now()
        );
    }
}
