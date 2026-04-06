package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestHistoryPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.domain.enums.HistoryAction;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.RequestHistory;
import co.edu.uniquindio.triage.domain.model.id.OriginChannelId;
import co.edu.uniquindio.triage.domain.model.id.RequestId;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;
import co.edu.uniquindio.triage.domain.model.id.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetRequestHistoryServiceTest {

    @Mock
    private LoadRequestPort loadRequestPort;

    @Mock
    private LoadRequestHistoryPort loadRequestHistoryPort;

    private GetRequestHistoryService service;

    @BeforeEach
    void setUp() {
        service = new GetRequestHistoryService(loadRequestPort, loadRequestHistoryPort);
    }

    @Test
    void shouldReturnHistoryWhenRequestExists() {
        // GIVEN
        var requestId = new RequestId(1L);
        var request = createSampleRequest(requestId);
        var expectedHistory = List.of(
                new RequestHistory(null, HistoryAction.REGISTERED, "Registered", LocalDateTime.now(), requestId, new UserId(10L)),
                new RequestHistory(null, HistoryAction.INTERNAL_NOTE, "Note", LocalDateTime.now(), requestId, new UserId(2L))
        );

        when(loadRequestPort.loadById(requestId)).thenReturn(Optional.of(request));
        when(loadRequestHistoryPort.loadRequestHistory(requestId)).thenReturn(expectedHistory);

        // WHEN
        var result = service.getRequestHistory(requestId);

        // THEN
        assertThat(result).isEqualTo(expectedHistory);
    }

    @Test
    void shouldThrowExceptionWhenRequestNotFound() {
        // GIVEN
        var requestId = new RequestId(1L);
        when(loadRequestPort.loadById(requestId)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> service.getRequestHistory(requestId))
                .isInstanceOf(RequestNotFoundException.class);

        verifyNoInteractions(loadRequestHistoryPort);
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
