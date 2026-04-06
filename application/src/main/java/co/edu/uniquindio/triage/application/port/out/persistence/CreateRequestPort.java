package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.AcademicRequest;

public interface CreateRequestPort {

    AcademicRequest create(NewAcademicRequest request);
}
