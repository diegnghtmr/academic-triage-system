package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.AcademicRequest;

public interface SaveRequestPort {

    void save(AcademicRequest request);
}
