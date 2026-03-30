package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.application.port.in.common.Page;
import co.edu.uniquindio.triage.application.port.in.request.RequestSummary;

public interface SearchRequestPort {

    Page<RequestSummary> search(RequestSearchCriteria criteria);
}
