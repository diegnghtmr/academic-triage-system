package co.edu.uniquindio.triage.application.port.out.persistence;

import co.edu.uniquindio.triage.domain.model.AcademicRequest;
import co.edu.uniquindio.triage.domain.model.id.RequestId;

import java.util.Optional;

/**
 * Port for loading AcademicRequest with a pessimistic write lock.
 * Must be called from within an active transaction that also calls SaveRequestPort.save()
 * to hold the lock across the full load-mutate-save boundary.
 */
public interface LoadRequestForMutationPort {

    Optional<AcademicRequest> loadByIdForMutation(RequestId requestId);
}
