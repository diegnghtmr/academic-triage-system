package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.RequestHistory;
import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.List;

public interface LoadRequestHistoryPort {
    List<RequestHistory> loadRequestHistory(RequestId requestId);
}
