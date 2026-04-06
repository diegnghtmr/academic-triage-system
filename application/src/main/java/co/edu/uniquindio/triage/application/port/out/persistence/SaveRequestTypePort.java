package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.RequestType;

public interface SaveRequestTypePort {

    RequestType saveRequestType(RequestType requestType);
}
