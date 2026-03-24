package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.application.port.in.request.RequestDetail;
import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.Optional;

public interface LoadRequestPort {

    Optional<AcademicRequest> loadById(RequestId requestId);

    Optional<RequestDetail> loadDetailById(RequestId requestId);
}
