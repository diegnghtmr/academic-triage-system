package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.application.port.in.request.RequestPage;
import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;

public interface SearchRequestPort {

    RequestPage<RequestSummary> search(RequestSearchCriteria criteria);
}
