package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.command.request.AddInternalNoteCommand;
import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.application.port.in.request.RequestHistoryDetail;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestForMutationPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.application.port.out.persistence.SaveRequestPort;
import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.enums.Role;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.model.*;
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
import java.util.List;
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
    private LoadRequestForMutationPort loadRequestForMutationPort;

    @Mock
    private SaveRequestPort saveRequestPort;

    private AddInternalNoteService service;

    @BeforeEach
    void setUp() {
        service = new AddInternalNoteService(loadRequestForMutationPort, loadRequestPort, saveRequestPort);
    }

    @Test
    void shouldAddInternalNoteSuccessfully() {
        // GIVEN
        var requestId = new RequestId(1L);
        var performedById = new UserId(2L);
        var note = "This is an internal note";
        var command = new AddInternalNoteCommand(requestId, note, performedById);

        var request = createSampleRequest(requestId);
        var performer = sampleUser(performedById.value(), "staff1", Role.STAFF);
        
        when(loadRequestForMutationPort.loadByIdForMutation(requestId)).thenReturn(Optional.of(request));
        
        // Mocking the reload of detail
        var detail = createSampleDetail(requestId, performer, note);
        when(loadRequestPort.loadDetailById(requestId)).thenReturn(Optional.of(detail));

        // WHEN
        var result = service.addInternalNote(command);

        // THEN
        var requestCaptor = ArgumentCaptor.forClass(AcademicRequest.class);
        verify(saveRequestPort).save(requestCaptor.capture());

        var savedRequest = requestCaptor.getValue();
        assertThat(savedRequest.getHistory()).hasSize(2);
        
        assertThat(result).isNotNull();
        assertThat(result.historyEntry().getAction()).isEqualTo(HistoryAction.INTERNAL_NOTE);
        assertThat(result.historyEntry().getObservations()).isEqualTo(note);
        assertThat(result.performedBy().getUsername().value()).isEqualTo("staff1");
    }

    @Test
    void shouldThrowExceptionWhenRequestNotFound() {
        // GIVEN
        var requestId = new RequestId(1L);
        var command = new AddInternalNoteCommand(requestId, "Note", new UserId(2L));
        when(loadRequestForMutationPort.loadByIdForMutation(requestId)).thenReturn(Optional.empty());

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

    private User sampleUser(long id, String username, Role role) {
        return User.reconstitute(
                new UserId(id),
                new Username(username),
                "Name",
                "Last",
                new PasswordHash("hash-valid-length-at-least-some-chars"),
                new Identification("12345678"),
                new Email(username + "@test.com"),
                role,
                true
        );
    }

    private RequestDetail createSampleDetail(RequestId id, User performer, String note) {
        var request = createSampleRequest(id);
        var type = new RequestType(new RequestTypeId(1L), "Type", "Desc", true);
        var channel = new OriginChannel(new OriginChannelId(1L), "Channel", true);
        var requester = sampleUser(10L, "student", Role.STUDENT);
        
        var entry = new RequestHistory(null, HistoryAction.INTERNAL_NOTE, note, LocalDateTime.now(), id, performer.getId().orElseThrow());
        var detailEntry = new RequestHistoryDetail(entry, performer);
        
        return new RequestDetail(request, type, channel, requester, Optional.empty(), List.of(detailEntry));
    }
}
