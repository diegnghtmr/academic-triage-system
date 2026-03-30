package co.edu.uniquindio.triage.application.service.request;

import co.edu.uniquindio.triage.application.port.in.request.GetRequestHistoryQuery;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestHistoryPort;
import co.edu.uniquindio.triage.application.port.out.persistence.LoadRequestPort;
import co.edu.uniquindio.triage.domain.exception.RequestNotFoundException;
import co.edu.uniquindio.triage.domain.model.RequestHistory;
import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.List;
import java.util.Objects;

public class GetRequestHistoryService implements GetRequestHistoryQuery {

    private final LoadRequestPort loadRequestPort;
    private final LoadRequestHistoryPort loadRequestHistoryPort;

    public GetRequestHistoryService(LoadRequestPort loadRequestPort, LoadRequestHistoryPort loadRequestHistoryPort) {
        this.loadRequestPort = Objects.requireNonNull(loadRequestPort, "El loadRequestPort no puede ser null");
        this.loadRequestHistoryPort = Objects.requireNonNull(loadRequestHistoryPort, "El loadRequestHistoryPort no puede ser null");
    }

    @Override
    public List<RequestHistory> getRequestHistory(RequestId requestId) {
        Objects.requireNonNull(requestId, "El requestId no puede ser null");

        if (loadRequestPort.loadById(requestId).isEmpty()) {
            throw new RequestNotFoundException(requestId);
        }

        return loadRequestHistoryPort.loadRequestHistory(requestId);
    }
}
