package co.edu.uniquindio.triage.application.port.in.request;

import co.edu.uniquindio.triage.domain.model.RequestHistory;
import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.List;

public interface GetRequestHistoryQuery {
    List<RequestHistory> getRequestHistory(RequestId requestId);
}
