package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.RequestType;
import co.edu.uniquindio.triage.domain.model.id.RequestTypeId;

import java.util.Optional;

public interface LoadRequestTypePort {

    Optional<RequestType> loadById(RequestTypeId requestTypeId);
}
